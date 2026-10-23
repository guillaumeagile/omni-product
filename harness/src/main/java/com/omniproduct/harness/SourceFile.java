package com.omniproduct.harness;

import org.treesitter.TSNode;
import org.treesitter.TSTree;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.stream.Stream;

// Un fichier source parsé par tree-sitter. Les offsets de tree-sitter sont des octets UTF-8 :
// le texte d'un noeud se lit donc dans le tableau d'octets, jamais dans la String.
public record SourceFile(String path, byte[] bytes, TSTree tree) {

    public Node root() {
        return new Node(this, tree.getRootNode());
    }

    public Stream<Node> descendants() {
        return root().descendants();
    }

    public String fileName() {
        return Path.of(path).getFileName().toString();
    }

    public String text(TSNode node) {
        return new String(bytes, node.getStartByte(), node.getEndByte() - node.getStartByte(), StandardCharsets.UTF_8);
    }

    public String packageName() {
        return root().children()
                .filter(node -> node.is("package_declaration"))
                .flatMap(Node::children)
                .filter(node -> node.is("scoped_identifier", "identifier"))
                .map(Node::text)
                .findFirst()
                .orElse("");
    }
}
