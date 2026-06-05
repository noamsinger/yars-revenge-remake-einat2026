package com.yarsrevenge.renderer;

import javafx.scene.canvas.GraphicsContext;

public interface Renderer {
    void render(GraphicsContext gc, RenderContext ctx);
}
