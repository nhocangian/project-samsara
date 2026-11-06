#version 120

// Lớp "mắt phát sáng" (RenderLayer.getEyes): vẽ như vanilla vào colortex0,
// đồng thời ghi độ sáng vào colortex4 để final.fsh làm quầng sáng (lib/eye_glow.glsl).
/* DRAWBUFFERS:04 */

uniform sampler2D tex;

varying vec2 texCoord;
varying vec4 color;

void main() {
    vec4 c = texture2D(tex, texCoord) * color;
    if (c.a < 0.02) discard;
    gl_FragData[0] = c;
    gl_FragData[1] = vec4(c.rgb * c.a, 1.0);
}
