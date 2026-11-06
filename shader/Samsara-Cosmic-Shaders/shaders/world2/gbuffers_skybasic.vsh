#version 120

varying vec4 starColor;

// Thêm: hướng nhìn để vẽ bầu trời thủ tục
varying vec3 viewPosSky;

void main() {
    gl_Position = ftransform();
    starColor = gl_Color;
    viewPosSky = (gl_ModelViewMatrix * gl_Vertex).xyz;
}
