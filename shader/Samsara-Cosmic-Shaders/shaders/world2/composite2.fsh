#version 120

/* DRAWBUFFERS:1 */

// Pass 3/4: làm mờ dọc (colortex2 -> colortex1). Sau pass này colortex1 là bloom hoàn chỉnh.

const bool colortex2MipmapEnabled = true;

uniform sampler2D colortex2;
uniform float viewHeight;

varying vec2 texCoord;

void main() {
    float stepY = 3.0 / viewHeight;
    float wts[5];
    wts[0] = 0.2270; wts[1] = 0.1945; wts[2] = 0.1216; wts[3] = 0.0541; wts[4] = 0.0162;

    vec3 sum = texture2D(colortex2, texCoord, 1.5).rgb * wts[0];
    for (int i = 1; i < 5; i++) {
        vec2 o = vec2(0.0, stepY * float(i) * 1.5);
        sum += texture2D(colortex2, texCoord + o, 1.5).rgb * wts[i];
        sum += texture2D(colortex2, texCoord - o, 1.5).rgb * wts[i];
    }
    gl_FragData[0] = vec4(sum, 1.0);
}
