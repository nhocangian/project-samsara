#version 120

/* DRAWBUFFERS:0 */

#include "/lib/loi_di_nhap_sky.glsl"

uniform mat4 gbufferModelViewInverse;
uniform mat4 gbufferProjectionInverse;
uniform vec3 cameraPosition;
uniform float frameTimeCounter;

varying vec4 vertexPos;

void main() {
    vec4 view = gbufferProjectionInverse * vertexPos;
    view /= view.w;
    vec3 dir = normalize((gbufferModelViewInverse * vec4(view.xyz, 0.0)).xyz);
    gl_FragData[0] = vec4(ldnSky(cameraPosition, dir, frameTimeCounter), 1.0);
}
