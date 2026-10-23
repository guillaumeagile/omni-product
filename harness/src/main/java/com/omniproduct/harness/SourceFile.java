package com.omniproduct.harness;

import org.treesitter.TSNode;
import org.treesitter.TSTree;

import java.nio.charset.StandardCharsets;

// Un fichier source parsé par tree-sitter. Les offsets de tree-sitter sont des octets UTF-8 :
// le texte d'un noeud se lit donc dans le tableau d'octets, jamais dans la String.
public record SourceFile(String path, byte[] bytes, TSTree tree) {

    public TSNode root() {
        return tree.getRootNode();
    }

    public String text(TSNode node) {
        return new String(bytes, node.getStartByte(), node.getEndByte() - node.getStartByte(), StandardCharsets.UTF_8);
    }

    public int line(TSNode node) {
        return node.getStartPoint().getRow() + 1;
    }

    public String packageName() {
        TSNode root = root();
        for (int i = 0; i < root.getNamedChildCount(); i++) {
            TSNode child = root.getNamedChild(i);
            if (child.getType().equals("package_declaration")) {
                for (int j = 0; j < child.getNamedChildCount(); j++) {
                    TSNode name = child.getNamedChild(j);
                    if (name.getType().equals("scoped_identifier") || name.getType().equals("identifier")) {
                        return text(name);
                    }
                }
            }
        }
        return "";
    }
}
