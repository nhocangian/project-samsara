#version 120

/* DRAWBUFFERS:2 */

// Pass 2/4: làm mờ ngang phần sáng (colortex1 -> colortex2).
// Đọc ở mip ~1.5 để mỗi mẫu đã là trung bình vài pixel -> bloom rộng mà rẻ.

const bool colortex1MipmapEnabled = true;

uniform sampler2D colortex1;
uniform float viewWidth;

varying vec2 texCoord;

void main() {
    float stepX = 3.0 / viewWidth;
    float wts[5];
    wts[0] = 0.2270; wts[1] = 0.1945; wts[2] = 0.1216; wts[3] = 0.0541; wts[4] = 0.0162;

    vec3 sum = texture2D(colortex1, texCoord, 1.5).rgb * wts[0];
    for (int i = 1; i < 5; i++) {
        vec2 o = vec2(stepX * float(i) * 1.5, 0.0);
        sum += texture2D(colortex1, texCoord + o, 1.5).rgb * wts[i];
        sum += texture2D(colortex1, texCoord - o, 1.5).rgb * wts[i];
    }
    gl_FragData[0] = vec4(sum, 1.0);
}
