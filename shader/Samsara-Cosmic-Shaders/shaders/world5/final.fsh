#version 120

// Tầng 3 "Trốn tìm": nền phải đen tuyền. Màu xoá mặc định của Iris (màu sương/trời) hơi xám nâu và lộ ra qua các khe
// li ti giữa các mặt khối đen, thành những chấm và vạch mờ dù không có tiếng động nào.
const vec4 colortex0ClearColor = vec4(0.0, 0.0, 0.0, 1.0);

uniform sampler2D colortex0;

varying vec2 texcoord;

void main() {
    gl_FragColor = vec4(texture2D(colortex0, texcoord).rgb, 1.0);
}
