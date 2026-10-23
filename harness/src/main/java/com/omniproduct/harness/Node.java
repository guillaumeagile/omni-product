package com.omniproduct.harness;

import org.treesitter.TSNode;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

// Un noeud de l'AST vu comme une collection : on le parcourt avec des streams, comme on le ferait en LINQ.
public record Node(SourceFile file, TSNode raw) {

    public String type() {
        return raw.getType();
    }

    public boolean is(String... types) {
        return Arrays.asList(types).contains(type());
    }

    public String text() {
        return file.text(raw);
    }

    public int line() {
        return raw.getStartPoint().getRow() + 1;
    }

    // Enfants nommés (identifiants, déclarations, ...) : l'essentiel de ce qu'une règle regarde.
    public Stream<Node> children() {
        return IntStream.range(0, raw.getNamedChildCount())
                .mapToObj(i -> new Node(file, raw.getNamedChild(i)));
    }

    // Tous les enfants, mots-clés compris ("public", "static", ...).
    public Stream<Node> tokens() {
        return IntStream.range(0, raw.getChildCount())
                .mapToObj(i -> new Node(file, raw.getChild(i)));
    }

    // Descendants en profondeur d'abord, sans le noeud lui-même.
    public Stream<Node> descendants() {
        return children().flatMap(child -> Stream.concat(Stream.of(child), child.descendants()));
    }

    public Stream<Node> selfAndDescendants() {
        return Stream.concat(Stream.of(this), descendants());
    }

    public Optional<Node> field(String name) {
        return Optional.ofNullable(raw.getChildByFieldName(name))
                .filter(node -> !node.isNull())
                .map(node -> new Node(file, node));
    }

    public boolean hasModifier(String modifier) {
        return tokens()
                .filter(token -> token.is("modifiers"))
                .flatMap(Node::tokens)
                .anyMatch(token -> token.is(modifier));
    }

    public Stream<Node> annotations() {
        return tokens()
                .filter(token -> token.is("modifiers"))
                .flatMap(Node::descendants)
                .filter(node -> node.is("marker_annotation", "annotation"));
    }
}
