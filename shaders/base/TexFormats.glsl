/*
const int colortex0Format = RGBA16F; // Main
const int colortex5Format = RGBA32UI;
const int colortex6Format = R32F; // ViewZ
const int colortex12Format = RGBA32UI; // TempUI
const int colortex13Format = RGBA16F; // Temp1
const int colortex14Format = RGBA16F; // Temp2
const int colortex15Format = RGBA16F; // TAA Last
*/

#define usam_main colortex0
#define uimg_main colorimg0

#define usam_gbuffer colortex5
#define uimg_gbuffer colorimg5

#define usam_viewZ colortex6
#define uimg_viewZ colorimg6

#define usam_tempUI colortex12
#define uimg_tempUI colorimg12

#define usam_temp1 colortex13
#define uimg_temp1 colorimg13

#define usam_temp2 colortex14
#define uimg_temp2 colorimg14

#define usam_taaLast colortex15