#include "Common.glsl"

void svgf_update(vec3 currColor, vec4 prevColorHLen, vec2 prevMoments, out float newHLen, out vec2 newMoments, out vec4 filterInput) {
    vec2 currMoments;
    currMoments.r = colors_srgbLuma(currColor);
    currMoments.g = currMoments.r * currMoments.r;

    if (prevColorHLen.a == 0.0) {
        newHLen = 1.0;
        newMoments = currMoments;
        filterInput.rgb = currColor;
    } else {
        newHLen = min(prevColorHLen.a + 1.0, SETTING_SVGF_MAX_ACCUM);
        float alpha = 1.0 / newHLen;
        newMoments = mix(prevMoments, currMoments, alpha);
        filterInput.rgb = mix(prevColorHLen.rgb, currColor, alpha);
    }

    float variance = max(newMoments.g - newMoments.r * newMoments.r, 0.0);
    filterInput.a = variance;
}