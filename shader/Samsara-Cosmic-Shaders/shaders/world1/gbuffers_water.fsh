#version 120

/* DRAWBUFFERS:0 */

uniform sampler2D tex;
uniform sampler2D lightmap;
uniform mat4 gbufferModelViewInverse;
uniform float frameTimeCounter;
uniform float far;
uniform int isEyeInWater;

#include "/lib/womb_atmosphere.glsl"

varying vec2 texCoord;
varying vec2 lmCoord;
varying vec3 normal;
varying vec4 color;
varying vec3 viewPos;

// =========================================================================
// 1. CÁC HÀM TOÁN HỌC & TIỆN ÍCH BẦU TRỜI VŨ TRỤ
// =========================================================================

float hash13(vec3 p) {
    vec3 p3 = fract(p * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

vec4 hash43(vec3 p) {
    vec4 p4 = fract(vec4(p.xyzx) * vec4(0.1031, 0.1030, 0.0973, 0.1099));
    p4 += dot(p4, p4.wzxy + 33.33);
    return fract((p4.xxyz + p4.yzzw) * p4.zywx);
}

float noise3D(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    
    float n000 = hash13(i + vec3(0.0, 0.0, 0.0));
    float n100 = hash13(i + vec3(1.0, 0.0, 0.0));
    float n010 = hash13(i + vec3(0.0, 1.0, 0.0));
    float n110 = hash13(i + vec3(1.0, 1.0, 0.0));
    float n001 = hash13(i + vec3(0.0, 0.0, 1.0));
    float n101 = hash13(i + vec3(1.0, 0.0, 1.0));
    float n011 = hash13(i + vec3(0.0, 1.0, 1.0));
    float n111 = hash13(i + vec3(1.0, 1.0, 1.0));

    float x00 = mix(n000, n100, f.x);
    float x10 = mix(n010, n110, f.x);
    float x01 = mix(n001, n101, f.x);
    float x11 = mix(n011, n111, f.x);

    float y0 = mix(x00, x10, f.y);
    float y1 = mix(x01, x11, f.y);

    return mix(y0, y1, f.z);
}

float fbm3D(vec3 p) {
    float v = 0.55 * noise3D(p);
    v += 0.30 * noise3D(p * 2.18 + vec3(1.3, 0.7, 2.4));
    v += 0.15 * noise3D(p * 4.50 + vec3(3.1, 2.2, 1.1));
    return v;
}

vec3 sampleStarsClean(vec3 dir, float t) {
    vec3 col = vec3(0.0);

    vec3 p1 = dir * 160.0;
    vec3 c1 = floor(p1);
    vec4 h1 = hash43(c1);
    if (h1.x > 0.965) {
        float d1 = length(p1 - (c1 + h1.yzw));
        float star1 = clamp(1.0 - d1 / 0.36, 0.0, 1.0);
        star1 *= star1;
        float twinkle = 0.75 + 0.35 * sin(h1.z * 60.0 + t * 2.2);
        vec3 tint1 = mix(vec3(1.0, 0.65, 0.30), vec3(0.75, 0.88, 1.0), h1.y);
        col += tint1 * star1 * twinkle * 1.8;
    }

    vec3 p2 = dir * 85.0;
    vec3 c2 = floor(p2);
    vec4 h2 = hash43(c2 + vec3(17.3, 31.7, 53.1));
    if (h2.x > 0.980) {
        float d2 = length(p2 - (c2 + h2.yzw));
        float star2 = clamp(1.0 - d2 / 0.40, 0.0, 1.0);
        star2 *= star2;
        float twinkle2 = 0.80 + 0.30 * sin(h2.w * 80.0 + t * 3.0);
        vec3 tint2 = mix(vec3(1.0, 0.85, 0.50), vec3(0.70, 0.85, 1.0), h2.y);
        col += tint2 * star2 * twinkle2 * 2.6;
    }

    return col;
}

vec3 KelvinToRgb(float Kelvin) {
    if (Kelvin < 400.0) return vec3(0.0);
    float Teff = (Kelvin - 6500.0) / (6500.0 * Kelvin * 2.2);
    vec3 col;
    col.r = exp(2.05539304e4 * Teff);
    col.g = exp(2.63463675e4 * Teff);
    col.b = exp(3.30145739e4 * Teff);
    float bScale = 1.0 / max(max(1.5 * col.r, col.g), col.b);
    if (Kelvin < 1000.0) bScale *= (Kelvin - 400.0) / 600.0;
    return col * bScale;
}

vec3 aces_tonemap(vec3 x) {
    const float a = 2.51;
    const float b = 0.03;
    const float c = 2.43;
    const float d = 0.59;
    const float e = 0.14;
    return clamp((x * (a * x + b)) / (x * (c * x + d) + e), 0.0, 1.0);
}

// =========================================================================
// 2. TÍNH TOÁN PHẢN CHIẾU HẮC LỖ GARGANTUA & BẦU TRỜI SAO TRÊN MẶT NƯỚC
// =========================================================================

vec3 renderCosmicReflection(vec3 rayWorld, float time) {
    vec3 bhForward = normalize(vec3(0.0, 0.54, -0.84));
    vec3 bhRight = normalize(cross(bhForward, vec3(0.0, 1.0, 0.0))); 
    vec3 bhUp = cross(bhRight, bhForward);

    float u_screen = dot(rayWorld, bhRight);
    float v_screen = dot(rayWorld, bhUp);
    float w_screen = dot(rayWorld, bhForward);

    float facingBH = smoothstep(0.05, 0.40, w_screen);

    // Nếu tia phản chiếu không hướng về Hắc Lỗ, chỉ vẽ bầu trời sao để tối ưu 100% hiệu năng
    if (facingBH <= 0.002) {
        vec3 bgStars = sampleStarsClean(rayWorld, time);
        float horizonGlow = smoothstep(0.0, 0.10, rayWorld.y) * smoothstep(0.35, 0.0, rayWorld.y);
        return aces_tonemap(bgStars + vec3(0.04, 0.015, 0.08) * horizonGlow);
    }

    vec3 camPos = vec3(0.0, 0.92, 21.0);
    vec3 camDir = normalize(vec3(0.0, -0.044, -1.0));

    float theta = -12.0 * 0.01745329252; // giống gbuffers_skybasic.fsh
    vec3 rawUp = vec3(-sin(theta), cos(theta), 0.0);
    vec3 camRight = normalize(cross(camDir, rawUp));
    vec3 camUp = normalize(cross(camRight, camDir));

    float viewScale = 1.25;
    vec3 rayDir = normalize(camDir + (u_screen * camRight + v_screen * camUp) * viewScale);

    float M = 1.0;
    float rH = 2.0 * M;
    float rISCO = 2.60;
    float rOut = 15.5;

    vec3 p = camPos;
    vec3 v = rayDir;

    vec3 radAccum = vec3(0.0);
    vec3 transmittance = vec3(1.0);
    bool trappedInHorizon = false;
    float minApproach = 999.0;

    // Raymarching quỹ đạo ánh sáng 46 bước cực kỳ mượt mà cho phản xạ mặt nước
    for (int step = 0; step < 46; step++) {
        float r = length(p);
        minApproach = min(minApproach, r);

        if (r <= rH * 1.015) {
            trappedInHorizon = true;
            break;
        }

        if (r > 36.0 && dot(p, v) > 0.0) {
            break;
        }

        float dt = clamp(r * 0.085, 0.025, 0.85);

        vec3 h = cross(p, v);
        float h2 = dot(h, h);
        vec3 aGrav = -1.5 * h2 * p / (r * r * r * r * r);

        vec3 pNext = p + v * dt + 0.5 * aGrav * dt * dt;
        vec3 vNext = normalize(v + aGrav * dt);

        // A. Cắt qua mặt phẳng đĩa xích đạo Y = 0
        if ((p.y * pNext.y) <= 0.0) {
            float denom = abs(pNext.y - p.y) + 1e-6;
            float tCross = clamp(-p.y / denom, 0.0, 1.0);
            vec3 pCross = p + tCross * (pNext - p);
            float rCross = length(pCross.xz);

            if (rCross >= rISCO && rCross <= rOut) {
                float phi = atan(pCross.z, pCross.x);
                float omega = 3.2 / (rCross * sqrt(rCross) + 0.25);
                float phiFlow = phi - time * omega;

                float spiral1 = sin(phiFlow * 2.0 - 2.6 * log(max(1.0, rCross)));
                float spiral2 = sin(phiFlow * 3.0 - 3.6 * log(max(1.0, rCross)) + 1.2);
                float armStreamers = pow(clamp(0.5 + 0.35 * spiral1 + 0.15 * spiral2, 0.0, 1.0), 1.6);

                float lanes = 1.0 + 0.22 * sin(rCross * 1.9 + 0.4 * spiral1) + 0.10 * sin(rCross * 4.0);

                float rho_r = smoothstep(rISCO, rISCO + 0.35, rCross)
                            * smoothstep(rOut, rOut - 4.5, rCross)
                            * pow(rISCO / rCross, 0.55);

                vec3 coord = vec3(rCross * 1.35 - time * 0.05, pCross.y * 5.5, phiFlow * 2.6);
                float rawSmoke = fbm3D(coord);
                float plasmaKnots = pow(rawSmoke, 2.4);
                float plasmaIntensity = 0.30 + 0.70 * plasmaKnots;

                float density = rho_r * lanes * (0.35 + 0.65 * armStreamers) * plasmaIntensity;

                float Omega = 1.0 / (rCross * sqrt(rCross));
                vec3 v_orb = vec3(pCross.z, 0.0, -pCross.x) / (rCross + 1e-5);
                float beta = clamp(rCross * Omega, 0.0, 0.75);
                float gamma = 1.0 / sqrt(max(0.01, 1.0 - beta * beta));
                float cosAlpha = dot(v_orb, normalize(pNext - p));
                float delta = 1.0 / max(0.05, gamma * (1.0 - beta * cosAlpha));
                float g_redshift = sqrt(max(0.01, 1.0 - rH / rCross));
                float g_factor = delta * g_redshift;

                float T_base = 7200.0 * pow(rISCO / rCross, 0.75);
                float T_obs = clamp(T_base * g_factor, 800.0, 48000.0);
                vec3 T_color = KelvinToRgb(T_obs);
                float dopplerBeaming = pow(clamp(g_factor, 0.25, 4.0), 3.2);

                float dl = length(pNext - p);
                float d_tau = 1.2 * density * dl;
                vec3 step_trans = exp(-d_tau * vec3(1.0, 1.05, 1.12));
                vec3 emission = T_color * (vec3(1.0) - step_trans) * dopplerBeaming * 4.9;

                radAccum += emission * transmittance;
                transmittance *= step_trans;
            }
        }

        // B. Khói bụi thể tích phát quang
        vec3 pMid = 0.5 * (p + pNext);
        float rMid = length(pMid.xz);
        if (rMid >= rISCO && rMid <= rOut) {
            float H = 0.065 * rMid + 0.02;
            float rho_y = exp(-abs(pMid.y) / H);
            float rho_r = smoothstep(rISCO, rISCO + 0.35, rMid) * smoothstep(rOut, rOut - 4.5, rMid);

            float phi = atan(pMid.z, pMid.x);
            float omega = 3.2 / (rMid * sqrt(rMid) + 0.25);
            float phiFlow = phi - time * omega;

            float spiral1 = sin(phiFlow * 2.0 - 2.6 * log(max(1.0, rMid)));
            float spiral2 = sin(phiFlow * 3.0 - 3.6 * log(max(1.0, rMid)) + 1.2);
            float armStreamers = pow(clamp(0.5 + 0.35 * spiral1 + 0.15 * spiral2, 0.0, 1.0), 1.6);
            float lanes = 1.0 + 0.22 * sin(rMid * 1.9 + 0.4 * spiral1) + 0.10 * sin(rMid * 4.0);

            vec3 coord = vec3(rMid * 1.35 - time * 0.05, pMid.y * 5.5, phiFlow * 2.6);
            float rawSmoke = fbm3D(coord);
            float plasmaKnots = pow(rawSmoke, 2.4);
            float plasmaIntensity = 0.30 + 0.70 * plasmaKnots;

            float density = rho_y * rho_r * lanes * (0.35 + 0.65 * armStreamers) * plasmaIntensity;

            float t_inner = clamp((4.4 - rMid) / 1.6, 0.0, 1.0);
            vec3 col_base = mix(vec3(1.10, 0.52, 0.18), vec3(3.80, 3.50, 3.20), t_inner);

            float dl = length(pNext - p);
            float d_tau_vol = 1.5 * density * dl;
            vec3 step_trans_vol = exp(-d_tau_vol * vec3(1.0, 1.05, 1.12));

            vec3 emission_vol = col_base * (vec3(1.0) - step_trans_vol) * 4.9;
            radAccum += emission_vol * transmittance;
            transmittance *= step_trans_vol;
        }

        p = pNext;
        v = vNext;
    }

    float distHorizon = max(0.0, minApproach - rH);
    float distCore = max(0.0, minApproach - 2.1);

    vec3 glowPhoton = exp(-distHorizon * 5.5) * vec3(4.50, 4.00, 3.50) * 3.6;
    vec3 glowCorona = (exp(-distCore * 0.45) * vec3(1.35, 0.90, 0.45) * 0.85 + 
                       exp(-distCore * 0.20) * vec3(0.85, 0.45, 0.18) * 0.25);

    vec3 celestialBloom = (glowPhoton + glowCorona) * facingBH;

    vec3 finalColor;
    if (trappedInHorizon) {
        finalColor = radAccum;
    } else {
        float blendLens = clamp((18.0 - minApproach) / 12.0, 0.0, 1.0);
        vec3 vStar = normalize(mix(rayWorld, v, blendLens));
        vec3 bgStars = sampleStarsClean(vStar, time);
        finalColor = bgStars * transmittance + radAccum + celestialBloom;
    }

    float horizonGlow = smoothstep(0.0, 0.10, rayWorld.y) * smoothstep(0.35, 0.0, rayWorld.y);
    finalColor += vec3(0.04, 0.015, 0.08) * horizonGlow;

    return aces_tonemap(finalColor);
}

// =========================================================================
// 3. MAIN SHADER: BIỂN ĐEN TĨNH LẶNG & PHẢN CHIẾU GƯƠNG VŨ TRỤ
// =========================================================================

void main() {
    vec4 albedo = texture2D(tex, texCoord);
    if (albedo.a < 0.02) discard;

    float t = frameTimeCounter;

    // A. Chuyển đổi tọa độ View Space sang World Space
    vec3 worldViewDir = normalize((gbufferModelViewInverse * vec4(viewPos, 0.0)).xyz);
    vec3 worldPos = (gbufferModelViewInverse * vec4(viewPos, 1.0)).xyz + cameraPosition;

    vec3 wNorm = (gbufferModelViewInverse * vec4(normal, 0.0)).xyz;
    vec3 worldNormal = length(wNorm) > 0.001 ? normalize(wNorm) : vec3(0.0, 1.0, 0.0);

    // B. Tạo gợn sóng chất lỏng vi sai tự nhiên (Gentle Natural Liquid Waves)
    vec3 N = worldNormal;
    float liquidCrest = 0.0;
    float crestGlint = 0.0;

    if (worldNormal.y > 0.55) {
        vec2 wp = worldPos.xz;
        float wt = t * 0.95;

        // 1. Các lớp sóng giao thoa êm đềm nhận biết mặt chất lỏng (Multi-layered gentle waves)
        float w1 = sin(wp.x * 0.75 + wt * 1.35 + wp.y * 0.40);
        float w2 = cos(wp.x * 0.45 - wt * 0.95 + wp.y * 0.65);
        float w3 = sin((wp.x + wp.y) * 1.10 - wt * 1.6);
        float waveHeight = (w1 + w2 + w3) / 3.0;

        vec2 waveSlope = vec2(
            0.75 * cos(wp.x * 0.75 + wt * 1.35 + wp.y * 0.40) - 0.45 * sin(wp.x * 0.45 - wt * 0.95 + wp.y * 0.65),
            0.40 * cos(wp.x * 0.75 + wt * 1.35 + wp.y * 0.40) - 0.65 * sin(wp.x * 0.45 - wt * 0.95 + wp.y * 0.65)
        ) * 0.038;

        N = normalize(vec3(waveSlope.x, 1.0, waveSlope.y));

        // Ánh sáng lấp lánh phản quang trên đỉnh gờ sóng (Liquid Surface Sheen)
        liquidCrest = smoothstep(0.20, 0.85, waveHeight) * 0.14;
    }

    // C. Vector phản xạ tia nhìn lên vòm trời vũ trụ
    vec3 reflRayWorld = reflect(worldViewDir, N);
    reflRayWorld.y = max(0.008, reflRayWorld.y);
    reflRayWorld = normalize(reflRayWorld);

    // D. Lấy màu phản chiếu của Hắc Lỗ & Bầu Trời Sao
    vec3 skyRefl = renderCosmicReflection(reflRayWorld, t);

    // E. Hệ số phản xạ Fresnel (Schlick Approximation cho mặt nước phản chiếu)
    float cosTheta = clamp(dot(N, -worldViewDir), 0.0, 1.0);
    float fresnel = 0.05 + 0.95 * pow(1.0 - cosTheta, 4.0);

    // F. Giữ nguyên hệ thống chiếu sáng biến đổi mà người dùng yêu thích:
    float blockLight = clamp((lmCoord.x - 0.03125) / 0.9375, 0.0, 1.0);
    float skyLight   = clamp((lmCoord.y - 0.03125) / 0.9375, 0.0, 1.0);

    vec3 bhDir = normalize(vec3(0.0, 0.54, -0.84));
    float NdotBH = dot(N, bhDir);
    float bhDiffuse = clamp(NdotBH * 0.65 + 0.35, 0.0, 1.0);
    vec3 bhLightCol = vec3(1.35, 0.95, 0.55);
    vec3 bhDirect = bhLightCol * bhDiffuse * clamp(skyLight * 1.1 + 0.30, 0.30, 1.30);

    float zenithFactor = clamp(N.y * 0.5 + 0.5, 0.25, 1.0);
    vec3 cosmicAmbient = vec3(0.25, 0.16, 0.10) * zenithFactor;

    float torchFactor = pow(blockLight, 2.0);
    vec3 soulTorch = WOMB_LAMP * (torchFactor * 1.6);   // ánh sáng block: trắng ngà, đứng yên (lib/womb_atmosphere.glsl)
    vec3 voidAmbient = vec3(0.06, 0.035, 0.05);

    vec3 totalLight = voidAmbient + cosmicAmbient + bhDirect + soulTorch;
    vec4 vanillaLight = texture2D(lightmap, lmCoord);
    totalLight *= clamp(vanillaLight.rgb * 1.2 + 0.20, 0.30, 1.40);

    // G. Màu cốt lõi của nước biển gương đen (Deep Obsidian Mirror Water)
    // Sắc thái mực đen vũ trụ hòa trộn ánh tím huyền bí, tạo cảm giác chất lỏng rõ rệt
    vec3 darkWaterFluid = mix(albedo.rgb * color.rgb, vec3(0.02, 0.045, 0.085), 0.70);
    vec3 waterBase = darkWaterFluid * totalLight;

    // Phản xạ vi mô từ đỉnh gờ sóng hướng về Gargantua
    crestGlint = pow(clamp(dot(N, normalize(bhDir + vec3(0.0, 0.8, 0.0))), 0.0, 1.0), 24.0) * 0.35;

    // H. Tổng hợp phản chiếu gương và ánh nước:
    // Thêm ánh sáng phản quang chất lỏng (liquidCrest & crestGlint) vào phần đáy nước
    vec3 liquidSurface = waterBase + vec3(0.04, 0.10, 0.18) * liquidCrest + vec3(1.2, 0.95, 0.6) * crestGlint;
    vec3 mirroredWater = mix(liquidSurface, skyRefl, fresnel);

    // Vệt sáng vàng kim lộng lẫy (Accretion Disk Sun-Glint / Specular Glade)
    vec3 halfVec = normalize(bhDir - worldViewDir);
    float specDisk = pow(clamp(dot(N, halfVec), 0.0, 1.0), 48.0);
    mirroredWater += vec3(1.85, 1.40, 0.85) * (specDisk * 1.5 * clamp(NdotBH, 0.0, 1.0));

    // Thêm quầng hào quang phản xạ cho các nguồn sáng rực rỡ
    mirroredWater += skyRefl * (0.35 * pow(1.0 - cosTheta, 2.0));

    // I. Sương mù vũ trụ tầm xa
    float dist = length(viewPos);
    float renderDist = far > 16.0 ? far : 192.0;
    float fogFactor = clamp(1.0 - exp(-pow(dist / (renderDist * 0.72), 2.4)), 0.0, 1.0);
    vec3 cosmicMist = mix(vec3(0.018, 0.006, 0.042), vec3(0.10, 0.03, 0.18), sin(t * 0.3 + dist * 0.015) * 0.5 + 0.5);

    vec3 finalRgb = mix(mirroredWater, cosmicMist, fogFactor);

    // Xử lý khi chìm dưới mặt nước (Submerged underwater view)
    if (isEyeInWater == 1) {
        float waterFogFactor = clamp(1.0 - exp(-dist * 0.055), 0.0, 1.0);
        vec3 underwaterFogColor = vec3(0.012, 0.045, 0.095);
        finalRgb = mix(finalRgb * vec3(0.40, 0.75, 1.15), underwaterFogColor, waterFogFactor);
    }

    // Alpha điều chỉnh theo Fresnel để đảm bảo mặt chất lỏng nhìn rõ ở mọi góc nhìn
    float finalAlpha = clamp(mix(0.82, 0.99, fresnel), 0.0, 1.0);

    gl_FragData[0] = vec4(finalRgb, finalAlpha);
}
