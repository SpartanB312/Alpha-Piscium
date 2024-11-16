#ifndef INCLUDE_util_BSDF.glsl
#define INCLUDE_util_BSDF.glsl

#include "Math.glsl"

// PPBR Diffuse Lighting for GGX+Smith Microsurfaces by Earl Hammon Jr.
// (https://www.gdcvault.com/play/1024478/PBR-Diffuse-Lighting-for-GGX)
vec3 bsdfs_diffuseHammon(float NDotL, float NDotV, float NDotH, float LDotV, vec3 albedo, float a) {
    if (NDotL <= 0.0) return vec3(0.0);
    float facing = 0.5 + 0.5 * LDotV;
    float singleRough = facing * (0.9 - 0.4 * facing) * (0.5 + NDotH) / NDotH;
    float singleSmooth = 1.05 * (1.0 - pow5(1.0 - NDotL)) * (1.0 - pow5(1.0 - NDotV));
    float single = mix(singleSmooth, singleRough, a) * RCP_PI_CONST;
    float multi = 0.1159 * a;
    return albedo * (single + albedo * multi);
}

#endif