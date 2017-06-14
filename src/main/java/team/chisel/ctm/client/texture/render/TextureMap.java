package team.chisel.ctm.client.texture.render;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.vecmath.Point2i;

import com.google.common.base.Preconditions;
import com.google.gson.JsonObject;

import lombok.Getter;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.util.math.BlockPos;
import team.chisel.ctm.api.texture.ISubmap;
import team.chisel.ctm.api.texture.ITextureContext;
import team.chisel.ctm.api.util.TextureInfo;
import team.chisel.ctm.client.texture.ctx.TextureContextGrid;
import team.chisel.ctm.client.texture.ctx.TextureContextPosition;
import team.chisel.ctm.client.texture.type.TextureTypeMap;
import team.chisel.ctm.client.util.Quad;
import team.chisel.ctm.client.util.Submap;

public class TextureMap extends AbstractTexture<TextureTypeMap> {

    public enum MapType {
        RANDOM {

            @Override
            protected List<BakedQuad> transformQuad(TextureMap tex, BakedQuad quad, @Nullable ITextureContext context, int quadGoal) {

                Point2i textureCoords = context == null ? new Point2i(1, 1) : ((TextureContextGrid)context).getTextureCoords(quad.getFace());
                
                float intervalX = 16f / tex.getXSize();
                float intervalY = 16f / tex.getYSize();
                
                float maxU = textureCoords.x * intervalX;
                float maxV = textureCoords.y * intervalY;
                ISubmap uvs = new Submap(intervalX, intervalY, maxU - intervalX, maxV - intervalY);

                Quad q = tex.makeQuad(quad, context).setFullbright(tex.fullbright);
                
                // TODO move this code somewhere else, it's copied from below
                if (quadGoal != 4) {
                    return Collections.singletonList(q.transformUVs(tex.sprites[0], uvs).setFullbright(tex.fullbright).rebake());
                } else {
                    Quad[] quads = q.subdivide(4);

                    for (int i = 0; i < quads.length; i++) {
                        if (quads[i] != null) {
                            quads[i] = quads[i].transformUVs(tex.sprites[0], uvs);
                        }
                    }
                    return Arrays.stream(quads).filter(Objects::nonNull).map(Quad::rebake).collect(Collectors.toList());
                }
            }
            
            @Override
            public ITextureContext getContext(@Nonnull BlockPos pos, @Nonnull TextureMap tex) {
                return new TextureContextGrid.Random(pos, tex, true);
            }
        },
        PATTERNED {

            @Override
            protected List<BakedQuad> transformQuad(TextureMap tex, BakedQuad quad, @Nullable ITextureContext context, int quadGoal) {
                
                Point2i textureCoords = context == null ? new Point2i(0, 0) : ((TextureContextGrid)context).getTextureCoords(quad.getFace());
                
                float intervalU = 16f / tex.xSize;
                float intervalV = 16f / tex.ySize;

                // throw new RuntimeException(index % variationSize+" and "+index/variationSize);
                float minU = intervalU * textureCoords.x;
                float minV = intervalV * textureCoords.y;

                ISubmap submap = new Submap(intervalU, intervalV, minU, minV);

                Quad q = tex.makeQuad(quad, context).setFullbright(tex.fullbright);
                if (quadGoal != 4) {
                    return Collections.singletonList(q.transformUVs(tex.sprites[0], submap).rebake());
                } else {
                    // Chisel.debug("V texture complying with quad goal of 4");
                    // Chisel.debug(new float[] { minU, minV, minU + intervalU, minV + intervalV });

                    Quad[] quads = q.subdivide(4);

                    for (int i = 0; i < quads.length; i++) {
                        if (quads[i] != null) {
                            quads[i] = quads[i].transformUVs(tex.sprites[0], submap);
                        }
                    }
                    return Arrays.stream(quads).filter(Objects::nonNull).map(Quad::rebake).collect(Collectors.toList());
                }
            }
            
            @Override
            public ITextureContext getContext(@Nonnull BlockPos pos, @Nonnull TextureMap tex) {
                return new TextureContextGrid.Patterned(pos, tex, true);
            }
        };

        protected abstract List<BakedQuad> transformQuad(TextureMap tex, BakedQuad quad, @Nullable ITextureContext context, int quadGoal);
        
        public ITextureContext getContext(@Nonnull BlockPos pos, @Nonnull TextureMap tex) {
            return new TextureContextPosition(pos);
        }
    }

    @Getter
    private final int xSize;
    @Getter
    private final int ySize;

    private final MapType map;

    public TextureMap(TextureTypeMap type, TextureInfo info, MapType map) {
        super(type, info);

        this.map = map;

        if (info.getInfo().isPresent()) {
            JsonObject object = info.getInfo().get();
            if (object.has("width") && object.has("height")) {
                Preconditions.checkArgument(object.get("width").isJsonPrimitive() && object.get("width").getAsJsonPrimitive().isNumber(), "width must be a number!");
                Preconditions.checkArgument(object.get("height").isJsonPrimitive() && object.get("height").getAsJsonPrimitive().isNumber(), "height must be a number!");

                this.xSize = object.get("width").getAsInt();
                this.ySize = object.get("height").getAsInt();

            } else if (object.has("size")) {
                Preconditions.checkArgument(object.get("size").isJsonPrimitive() && object.get("size").getAsJsonPrimitive().isNumber(), "size must be a number!");

                this.xSize = object.get("size").getAsInt();
                this.ySize = object.get("size").getAsInt();
            } else {
                xSize = ySize = 2;
            }
        } else {
            xSize = ySize = 2;
        }

        Preconditions.checkArgument(xSize > 0 && ySize > 0, "Cannot have a dimension of 0!");
    }

    @Override
    public List<BakedQuad> transformQuad(BakedQuad quad, ITextureContext context, int quadGoal) {
        return map.transformQuad(this, quad, context, quadGoal);
    }
}
