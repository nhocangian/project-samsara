// Tra bản đồ bóng cho chiều Ký Ức Của Nam (world2). CHỈ được include từ shaders/world2/*.fsh.
// Cần include "/lib/nam_memory_common.glsl" trước file này.
#ifndef NAM_MEMORY_SHADOW_GLSL
#define NAM_MEMORY_SHADOW_GLSL

uniform sampler2D shadowtex0;
uniform mat4 shadowModelView;
uniform mat4 shadowProjection;

// Nhiễu xoay mẫu PCF theo pixel (interleaved gradient noise)
float nmIGN(vec2 fragCoord) {
    return fract(52.9829189 * fract(dot(fragCoord, vec2(0.06711056, 0.00583715))));
}

// playerPos    : vị trí (đã bẻ cong) trong không gian người chơi
// normalPlayer : pháp tuyến trong không gian người chơi
// NdotL        : dùng để lật pháp tuyến lá cỏ về phía nắng
// trả về 0 (bóng) .. 1 (nắng)
float nmShadow(vec3 playerPos, vec3 normalPlayer, float NdotL, vec2 fragCoord) {
    // Ngoài vùng bản đồ bóng -> coi như được nắng (mờ dần ở rìa)
    float distH = length(playerPos.xz);
    float fadeOut = smoothstep(NM_SHADOW_DISTANCE * 0.80, NM_SHADOW_DISTANCE * 0.97, distH);
    if (fadeOut >= 1.0) return 1.0;

    // Bias theo pháp tuyến, tỉ lệ với kích thước texel bóng tại điểm đó
    vec4 c0 = shadowProjection * (shadowModelView * vec4(playerPos, 1.0));
    float df = nmShadowDistortFactor(c0.xy);
    float texelWorld = (2.0 * NM_SHADOW_DISTANCE / NM_SHADOW_RES) * df;
    vec3 n = normalPlayer * (NdotL < 0.0 ? -1.0 : 1.0);
    vec3 biased = playerPos + n * (texelWorld * 1.6 + 0.02);

    vec4 clip = shadowProjection * (shadowModelView * vec4(biased, 1.0));
    vec3 sp = nmDistortShadow(clip.xyz) * 0.5 + 0.5;
    if (sp.x <= 0.0 || sp.x >= 1.0 || sp.y <= 0.0 || sp.y >= 1.0 || sp.z >= 1.0) return 1.0;

    float refDepth = sp.z - 0.00006;

    // PCF 12 mẫu Poisson xoay ngẫu nhiên -> mép bóng mềm vừa phải, vẫn sắc nét
    float ang = nmIGN(fragCoord) * 6.2831853;
    mat2 rot = mat2(cos(ang), sin(ang), -sin(ang), cos(ang));
    float radius = 1.35 / NM_SHADOW_RES;

    vec2 poisson[12];
    poisson[0]  = vec2(-0.326, -0.406);
    poisson[1]  = vec2(-0.840, -0.074);
    poisson[2]  = vec2(-0.696,  0.457);
    poisson[3]  = vec2(-0.203,  0.621);
    poisson[4]  = vec2( 0.962, -0.195);
    poisson[5]  = vec2( 0.473, -0.480);
    poisson[6]  = vec2( 0.519,  0.767);
    poisson[7]  = vec2( 0.185, -0.893);
    poisson[8]  = vec2( 0.507,  0.064);
    poisson[9]  = vec2( 0.896,  0.412);
    poisson[10] = vec2(-0.322, -0.933);
    poisson[11] = vec2(-0.792, -0.598);

    float lit = 0.0;
    for (int i = 0; i < 12; i++) {
        vec2 o = rot * poisson[i] * radius;
        lit += step(refDepth, texture2D(shadowtex0, sp.xy + o).r);
    }
    lit /= 12.0;

    return mix(lit, 1.0, fadeOut);
}

#endif
