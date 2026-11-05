package com.mbolt.casino.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Splits the existing skinned cube at its knee; UVs, armor trims, colors and glint remain the original ones. */
public final class SeatedLegRenderer {
    private static final float KNEE = 6 / 16f;
    private static final float OVERLAP = 1 / 16f;
    private static final PoseStack LOCAL = new PoseStack();

    private SeatedLegRenderer() {
    }

    public static void render(ModelPart part, List<ModelPart.Cube> cubes, Map<String, ModelPart> children,
                              PoseStack poses, VertexConsumer consumer, int light, int overlay, int color) {
        if (!part.visible) {
            return;
        }
        poses.pushPose();
        part.translateAndRotate(poses);
        if (!part.skipDraw) {
            var thigh = new ClippedConsumer(poses.last().copy(), consumer, KNEE + OVERLAP, true);
            for (var cube : cubes) {
                cube.compile(LOCAL.last(), thigh, light, overlay, color);
            }
            poses.pushPose();
            poses.translate(0, KNEE, 0);
            poses.mulPose(Axis.XP.rotationDegrees(90));
            poses.translate(0, -KNEE, 0);
            var shin = new ClippedConsumer(poses.last().copy(), consumer, KNEE - OVERLAP, false);
            for (var cube : cubes) {
                cube.compile(LOCAL.last(), shin, light, overlay, color);
            }
            poses.popPose();
        }
        children.values().forEach(child -> child.render(poses, consumer, light, overlay, color));
        poses.popPose();
    }

    private record Vertex(float x, float y, float z, float u, float v) {
        Vertex between(Vertex other, float yPlane) {
            float t = (yPlane - y) / (other.y - y);
            return new Vertex(x + (other.x - x) * t, yPlane, z + (other.z - z) * t,
                    u + (other.u - u) * t, v + (other.v - v) * t);
        }
    }

    private static final class ClippedConsumer implements VertexConsumer {
        private final PoseStack.Pose pose;
        private final VertexConsumer target;
        private final float plane;
        private final boolean upper;
        private final List<Vertex> face = new ArrayList<>(4);

        ClippedConsumer(PoseStack.Pose pose, VertexConsumer target, float plane, boolean upper) {
            this.pose = pose;
            this.target = target;
            this.plane = plane;
            this.upper = upper;
        }

        @Override
        public void addVertex(float x, float y, float z, int color, float u, float v, int overlay, int light,
                              float nx, float ny, float nz) {
            face.add(new Vertex(x, y, z, u, v));
            if (face.size() != 4) {
                return;
            }
            List<Vertex> clipped = new ArrayList<>(4);
            Vertex previous = face.getLast();
            for (Vertex current : face) {
                boolean before = inside(previous);
                boolean after = inside(current);
                if (before != after) {
                    clipped.add(previous.between(current, plane));
                }
                if (after) {
                    clipped.add(current);
                }
                previous = current;
            }
            if (clipped.size() == 4) {
                Vector3f normal = pose.transformNormal(nx, ny, nz, new Vector3f());
                Vector3f point = new Vector3f();
                for (Vertex vertex : clipped) {
                    pose.pose().transformPosition(vertex.x, vertex.y, vertex.z, point);
                    target.addVertex(point.x, point.y, point.z, color, vertex.u, vertex.v, overlay, light,
                            normal.x, normal.y, normal.z);
                }
            }
            face.clear();
        }

        private boolean inside(Vertex vertex) {
            return upper ? vertex.y <= plane : vertex.y >= plane;
        }

        // Cube.compile uses the complete-vertex overload above. The streaming methods are deliberately unused.
        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            return this;
        }
        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            return this;
        }
        @Override
        public VertexConsumer setUv(float u, float v) {
            return this;
        }
        @Override
        public VertexConsumer setUv1(int u, int v) {
            return this;
        }
        @Override
        public VertexConsumer setUv2(int u, int v) {
            return this;
        }
        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            return this;
        }
    }
}
