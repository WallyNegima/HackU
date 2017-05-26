#include <Adafruit_NeoPixel.h>
#include <Adafruit_NeoPixel.h>
#ifdef __AVR__
  #include <avr/power.h>
#endif

#define PIN 13
#define NUM_LEDS 20
#define BRIGHTNESS 50
Adafruit_NeoPixel strip = Adafruit_NeoPixel(NUM_LEDS, PIN, NEO_RGB + NEO_KHZ800);
//何故かRGBなのに GRBになってる

byte neopix_gamma[] = {
    0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,
    0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  1,  1,  1,  1,
    1,  1,  1,  1,  1,  1,  1,  1,  1,  2,  2,  2,  2,  2,  2,  2,
    2,  3,  3,  3,  3,  3,  3,  3,  4,  4,  4,  4,  4,  5,  5,  5,
    5,  6,  6,  6,  6,  7,  7,  7,  7,  8,  8,  8,  9,  9,  9, 10,
   10, 10, 11, 11, 11, 12, 12, 13, 13, 13, 14, 14, 15, 15, 16, 16,
   17, 17, 18, 18, 19, 19, 20, 20, 21, 21, 22, 22, 23, 24, 24, 25,
   25, 26, 27, 27, 28, 29, 29, 30, 31, 32, 32, 33, 34, 35, 35, 36,
   37, 38, 39, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 50,
   51, 52, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 66, 67, 68,
   69, 70, 72, 73, 74, 75, 77, 78, 79, 81, 82, 83, 85, 86, 87, 89,
   90, 92, 93, 95, 96, 98, 99,101,102,104,105,107,109,110,112,114,
  115,117,119,120,122,124,126,127,129,131,133,135,137,138,140,142,
  144,146,148,150,152,154,156,158,160,162,164,167,169,171,173,175,
  177,180,182,184,186,189,191,193,196,198,200,203,205,208,210,213,
  215,218,220,223,225,228,231,233,236,239,241,244,247,249,252,255 };
int delayval = 500; // delay for half a second
int wait = 100;
int shu_wait = 50;
int shu_tail = 3;

int mycolor = 10;

void setup() {
  // put your setup code here, to run once:
  Serial.begin(115200);
  #if defined (__AVR_ATtiny85__)
    if (F_CPU == 16000000) clock_prescale_set(clock_div_1);
  #endif
  strip.setBrightness(BRIGHTNESS);
  strip.begin();
  strip.show();
  pika_red();
}

void loop() {
  // put your main code here, to run repeatedly:
  
  pika_red();
  delay(delayval);
  no_light();
  delay(delayval);
  shu_red();
  delay(delayval);
  pika_green();
  delay(delayval);
  no_light();
  delay(delayval);
  pika_blue();
  delay(delayval);
  no_light();
  delay(delayval);
  
  //pika_rainbow();
  //delay(delayval);
}

void pika_red(){
  for(int i=0; i<strip.numPixels(); i++){
    strip.setPixelColor(i, strip.Color(0,255,0));
    strip.show();
  }
}

void pika_green(){
  for(int i=0; i<strip.numPixels(); i++){
    strip.setPixelColor(i, strip.Color(255,0,0));
    strip.show();
  }
}

void pika_blue(){
  for(int i=0; i<strip.numPixels(); i++){
    strip.setPixelColor(i, strip.Color(0,0,255));
    strip.show();
  }
}

void pika_rainbow() {
  uint16_t i, j;

  for(j=0; j<256; j++) {
    for(i=0; i<strip.numPixels(); i++) {
      strip.setPixelColor(i, Wheel((i+j) & 255));
    }
    strip.show();
    delay(wait);
  }
}

void pika_rainbow2(){
  int i, j;

  for(j=0; j<256; j++) {
    for(i=0; i<strip.numPixels(); i++) {
      strip.setPixelColor(i, Wheel((i+j) & 255));
    }
    strip.show();
    delay(wait);
  }
}

void shu_red(){
  for(int i=0; i<strip.numPixels()+shu_tail+1; i++){
    if(i>=strip.numPixels()){
      //先頭が全体のLED数の先を行く時
      strip.setPixelColor(i-(shu_tail+1), strip.Color(0,0,0));
      strip.show();
    }else{
      strip.setPixelColor(i, strip.Color(0,255,0));
      strip.show();
      if(i < shu_tail+1){
        //何もせず
      }else{
        strip.setPixelColor(i-(shu_tail+1), strip.Color(0,0,0));
        strip.show();
      }
    }
    delay(shu_wait);
  }
}


void ziwaziwa(){
  for(int j=0; j<256; j++){
    for(int i=0; i<strip.numPixels(); i++){
      //strip.setPixel(i, Wheel(mycolor & 255));
    }
    strip.show();
  }
}

void no_light(){
  for(int i=0; i<strip.numPixels(); i++){
    strip.setPixelColor(i,strip.Color(0,0,0));
    strip.show();
  }
}

uint32_t Wheel(byte WheelPos) {
  WheelPos = 255 - WheelPos;
  if(WheelPos < 85) {
    return strip.Color(0,255 - WheelPos * 3, WheelPos * 3,0);
  }
  if(WheelPos < 170) {
    WheelPos -= 85;
    return strip.Color(WheelPos * 3, 0, 255 - WheelPos * 3,0);
  }
  WheelPos -= 170;
  return strip.Color(WheelPos * 3, 255 - WheelPos * 3, 0,0);
}

