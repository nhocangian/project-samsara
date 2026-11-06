#version 120

/* DRAWBUFFERS:0 */

#include "/lib/eye_glow.glsl"

uniform sampler2D colortex0;
uniform float frameTimeCounter;
uniform float darknessFactor;
uniform float blindness;
uniform int heldItemId;
uniform int heldItemId2;
uniform float nightVision;

varying vec2 texCoord;

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

void main() {
    bool isCamcorderAiming = (heldItemId == 1 || heldItemId2 == 1);
    bool isHoldingArtifact = isCamcorderAiming || (heldItemId == 2 || heldItemId2 == 2);
    bool isNightVisionMode = isCamcorderAiming && (nightVision > 0.5);

    float glitchIntensity = clamp(darknessFactor, 0.0, 1.0);
    float desyncIntensity = clamp(blindness, 0.0, 1.0);

    // Kiểm tra tối ưu hóa: Khi không có sự kiện ARG nào, xuất ảnh trực tiếp 100% không tốn hiệu năng
    if (!isHoldingArtifact && glitchIntensity <= 0.001 && desyncIntensity <= 0.001) {
        gl_FragData[0] = vec4(texture2D(colortex0, texCoord).rgb + eyeGlow(texCoord), 1.0);
        return;
    }

    vec2 uv = texCoord;
    float t = frameTimeCounter;

    // =========================================================================
    // HIỆU ỨNG 1: ANALOG HORROR / CUỘN BĂNG VHS (Khi cầm Fractured Slate / Journal)
    // =========================================================================
    if (isHoldingArtifact) {
        vec2 cc = uv - 0.5;
        // Cong góc màn hình kiểu bóng đèn hình CRT cổ điển
        uv = uv + cc * (dot(cc, cc) * 0.05);

        // Vệt nhiễu từ tính cuộn dọc theo băng từ (Magnetic Tracking Jitter)
        float tapeBand = fract(t * 0.18);
        float tapeDist = abs(uv.y - tapeBand);
        float tapeJitter = smoothstep(0.045, 0.0, tapeDist) * sin(uv.y * 140.0 + t * 40.0) * 0.008;
        uv.x += tapeJitter;
    }

    // =========================================================================
    // HIỆU ỨNG 2: REALITY GLITCH / MẤT ĐỒNG BỘ THỰC TẠI (Khi nhìn The Wanderer)
    // =========================================================================
    if (glitchIntensity > 0.001) {
        // Xé rách pixel ngang theo từng khối (Horizontal Pixel Sorting Glitch)
        float blockY = floor(uv.y * 28.0);
        float noiseBlock = hash(vec2(blockY, floor(t * 18.0)));
        if (noiseBlock > 0.62) {
            uv.x += (noiseBlock - 0.5) * (0.07 * glitchIntensity);
        }
    }

    // =========================================================================
    // HIỆU ỨNG 4: TEMPORAL DESYNC / HỒN LÌA THỂ XÁC (Khi Sleep Out-of-Body)
    // =========================================================================
    vec2 toCenter = uv - 0.5;
    float radialDisp = desyncIntensity * 0.025;

    // Tách kênh quang phổ RGB (Chromatic Dispersion)
    vec2 redOffset   = isHoldingArtifact ? vec2(0.003, 0.0) : vec2(0.0);
    vec2 blueOffset  = isHoldingArtifact ? vec2(-0.003, 0.0) : vec2(0.0);

    if (glitchIntensity > 0.001) {
        redOffset  += vec2(0.012 * glitchIntensity, 0.0);
        blueOffset -= vec2(0.012 * glitchIntensity, 0.0);
    }
    if (desyncIntensity > 0.001) {
        redOffset  += toCenter * radialDisp;
        blueOffset -= toCenter * radialDisp;
    }

    float r = texture2D(colortex0, uv + redOffset).r;
    float g = texture2D(colortex0, uv).g;
    float b = texture2D(colortex0, uv + blueOffset).b;
    vec3 sceneCol = vec3(r, g, b);

    // Radial ghosting blur khi xuất hồn
    if (desyncIntensity > 0.001) {
        vec3 ghostAccum = sceneCol;
        for (int i = 1; i <= 3; i++) {
            vec2 sampleUV = uv - toCenter * (float(i) * 0.012 * desyncIntensity);
            ghostAccum += texture2D(colortex0, sampleUV).rgb;
        }
        sceneCol = mix(sceneCol, ghostAccum / 4.0, desyncIntensity * 0.85);
    }

    // =========================================================================
    // BỔ SUNG CHI TIẾT TỪNG PHONG CÁCH:
    // =========================================================================
    if (isHoldingArtifact) {
        if (isNightVisionMode) {
            // =====================================================================
            // OUTLAST CHẾ ĐỘ HỒNG NGOẠI BAN ĐÊM (IR NIGHT VISION)
            // =====================================================================
            // 1. Chuyển đổi màu sắc sang cường độ sáng hồng ngoại (Grayscale Luminance)
            float lum = dot(sceneCol, vec3(0.299, 0.587, 0.114));

            // 2. Khuếch đại sáng trong bóng tối (High-contrast IR Gamma Boost)
            lum = pow(lum, 0.65) * 1.45;

            // 3. Phối màu lân tinh xanh lục biểu tượng của Outlast Camcorder (Phosphor Green)
            vec3 nvColor = vec3(lum * 0.22, lum * 1.38, lum * 0.42);

            // 4. Chùm sáng hồng ngoại đồng trục (Conical IR Spotlight Beam)
            vec2 centerDiff = uv - 0.5;
            float centerDist = length(centerDiff);
            float irSpotlight = clamp(1.28 - centerDist * 0.95, 0.35, 1.32);
            nvColor *= irSpotlight;

            // 5. Nhiễu hạt cảm biến hồng ngoại nhiệt độ cao (Thermal Sensor Noise)
            float irGrain = (hash(uv * 180.0 + fract(t * 24.0)) - 0.5) * 0.14;
            nvColor += irGrain;

            // 6. Quét tia điện tử CRT mỏng
            float scanline = sin(uv.y * 720.0) * 0.05;
            nvColor -= scanline;

            sceneCol = nvColor;
        } else {
            // =====================================================================
            // CHẾ ĐỘ THƯỜNG: ANALOG HORROR VHS TAPE (FOUND FOOTAGE)
            // =====================================================================
            float scanline = sin(uv.y * 720.0) * 0.06;
            sceneCol -= scanline;

            float grain = (hash(uv * 120.0 + fract(t * 15.0)) - 0.5) * 0.07;
            sceneCol += grain;

            // Ám sắc lân tinh nhẹ cổ điển
            sceneCol *= vec3(0.96, 1.03, 0.96);
        }

        // Chấm đỏ REC nhấp nháy ở góc trên trái
        float recBlink = step(0.5, fract(t * 1.0));
        if (texCoord.x > 0.025 && texCoord.x < 0.045 && texCoord.y > 0.94 && texCoord.y < 0.975) {
            sceneCol = mix(sceneCol, vec3(0.95, 0.1, 0.1), recBlink * 0.9);
        }
    }

    // Chi tiết Glitch: Chớp âm bản (Subliminal Negative Flash)
    if (glitchIntensity > 0.001) {
        float flashCycle = fract(t * 4.0);
        if (flashCycle < 0.15 * glitchIntensity) {
            sceneCol = vec3(1.0) - sceneCol; // Âm bản đảo ngược thực tại!
            sceneCol *= vec3(1.15, 0.75, 1.35); // Nhuốm sắc tím kinh hoàng
        }
        // Nhiễu nhị phân / static trong bóng tối
        float staticNoise = (hash(uv * 60.0 + t * 45.0) - 0.5) * (0.22 * glitchIntensity);
        sceneCol += staticNoise;
    }

    // Quầng sáng mắt / vật phát sáng (lib/eye_glow.glsl), theo đúng chỗ ảnh đã bị méo
    sceneCol += eyeGlow(uv);
    gl_FragData[0] = vec4(sceneCol, 1.0);
}
