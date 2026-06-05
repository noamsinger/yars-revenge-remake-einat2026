package com.yarsrevenge.screen;

import javafx.scene.Node;

public interface Screen {
    void show();
    void hide();
    Node getRoot();
}
