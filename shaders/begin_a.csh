#version 460 compatibility
#include "rtwsm/RTWSM.glsl"

const ivec3 workGroups = ivec3(WORKGROUP16_COUNT, WORKGROUP16_COUNT, 1);

layout(r32ui) uniform writeonly uimage2D uimg_rtwsm_imap2D;
#define CLEAR_IMAGE1 uimg_rtwsm_imap2D
const ivec4 CLEAR_IMAGE_BOUND = ivec4(0, 0, SETTING_RTWSM_IMAP_SIZE, SETTING_RTWSM_IMAP_SIZE);
const uvec4 CLEAR_COLOR1 = uvec4(0u);

#include "general/Clear1.comp"