package com.omniproduct.harness.referee;

import com.omniproduct.harness.referee.Leaderboard.Author;
import com.omniproduct.harness.referee.Leaderboard.Comment;
import com.omniproduct.harness.referee.Leaderboard.PullRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LeaderboardTest {

    private static final Author BOT = new Author("github-actions");

    private static Comment referee(String target, String outcome, int score, String level, String badges) {
        return new Comment(BOT, "## 🎮 CUPID referee\n\n" + Scorecard.MARKER
                + "{\"target\":\"" + target + "\",\"outcome\":\"" + outcome + "\",\"score\":" + score
                + ",\"level\":\"" + level + "\",\"badges\":[" + badges + "]} -->\n");
    }

    @Test
    void ranksTeamsByCumulatedScoreFromTheLastRefereeCommentOfEachMergedPullRequest() {
        List<PullRequest> merged = List.of(
                new PullRequest(1, "team-red/step-1/ha9", List.of(
                        referee("HA9", "REJECTED", 0, "Unix Apprentice (2/9)", ""),
                        referee("HA9", "ACCEPTED", 15, "Unix Apprentice (3/9)", "\"🍼 Baby step\""))),
                new PullRequest(2, "team-blue/step-1/ha8", List.of(
                        referee("HA8", "ACCEPTED", 18, "Unix Apprentice (3/9)", "\"🍼 Baby step\",\"🤏 Small diff\""))),
                new PullRequest(3, "team-red/step-1/ha10", List.of(
                        referee("HA10", "ACCEPTED", 15, "Single Concern Adept (4/9)", "\"🍼 Baby step\""))));

        String markdown = Leaderboard.markdown(merged);

        assertThat(markdown).contains(
                "| 🥇 | team-red | 30 | 2 | Single Concern Adept (4/9) | 🍼 Baby step ×2 |",
                "| 🥈 | team-blue | 18 | 1 | Unix Apprentice (3/9) | 🍼 Baby step ×1, 🤏 Small diff ×1 |");
    }

    @Test
    void ignoresAMarkerWrittenByAStudent() {
        Comment forged = new Comment(new Author("student"), Scorecard.MARKER
                + "{\"target\":\"HA9\",\"outcome\":\"ACCEPTED\",\"score\":999,\"level\":\"CUPID Master (9/9)\",\"badges\":[]} -->");

        String markdown = Leaderboard.markdown(List.of(new PullRequest(4, "team-green/step-1/ha9", List.of(forged))));

        assertThat(markdown).contains("_No merged baby step yet._").doesNotContain("999");
    }
}
