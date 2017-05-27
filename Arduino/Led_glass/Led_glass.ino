
#include <EEPROM.h>
#include <Wire.h>
#include "I2Cdev.h"
#include "MPU6050_6Axis_MotionApps20.h"
#include "Wire.h"


#include <Adafruit_NeoPixel.h>//neopixelライブラリ群
#ifdef __AVR__
#include <avr/power.h>
#endif

#define INTERRUPT_PIN 2//ピン番号定義
#define CMD 7
#define POWER_ON 9
#define TAPE_LED_PIN 12
#define LED_PIN 13

#define CANPAI_THRESHOLD 45000000//加速度閾値など
#define CANPAI_WAITTIME 500
#define Array 100

#define NUM_LEDS 20//neopixel設定
#define BRIGHTNESS 50
Adafruit_NeoPixel strip = Adafruit_NeoPixel(NUM_LEDS, TAPE_LED_PIN, NEO_RGB + NEO_KHZ800);

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
  90, 92, 93, 95, 96, 98, 99, 101, 102, 104, 105, 107, 109, 110, 112, 114,
  115, 117, 119, 120, 122, 124, 126, 127, 129, 131, 133, 135, 137, 138, 140, 142,
  144, 146, 148, 150, 152, 154, 156, 158, 160, 162, 164, 167, 169, 171, 173, 175,
  177, 180, 182, 184, 186, 189, 191, 193, 196, 198, 200, 203, 205, 208, 210, 213,
  215, 218, 220, 223, 225, 228, 231, 233, 236, 239, 241, 244, 247, 249, 252, 255
};

int delayval = 500; //neopixcel設定
int wait = 100;
int shu_wait = 50;
int shu_tail = 3;
int mycolor = 10;


int counter = 1, counterold = 1, canpaicounter = 1, canpaicounterold = 1;//加速度センサー用フラグ
int loopcounter, canpailoopcounter;
unsigned long time0, canpaitime0;

struct accelFlags {//加速度センサー用フラグ構造体
  int counter, counter_old, loopcounter;
  bool enter = false;
  bool enter_start = false;
  bool enter_end = false;
  bool enter_wait = false;
  unsigned long time0;
} canpai, ikki;

MPU6050 mpu;
bool blinkState = false;

// MP                                                      U control/status vars
bool dmpReady = false;  // set true if DMP init was successful
uint8_t mpuIntStatus;   // holds actual interrupt status byte from MPU
uint8_t devStatus;      // return status after each device operation (0 = success, !0 = error)
uint16_t packetSize;    // expected DMP packet size (default is 42 bytes)
uint16_t fifoCount;     // count of all bytes currently in FIFO
uint8_t fifoBuffer[64]; // FIFO storage buffer

// orientation/motion vars
Quaternion q;           // [w, x, y, z]         quaternion container
VectorInt16 aa;         // [x, y, z]            accel sensor measurements
VectorInt16 aaReal;     // [x, y, z]            gravity-free accel sensor measurements
VectorInt16 aaWorld;    // [x, y, z]            world-frame accel sensor measurements
VectorFloat gravity;    // [x, y, z]            gravity vector
float euler[3];         // [psi, theta, phi]    Euler angle container
float ypr[3];           // [yaw, pitch, roll]   yaw/pitch/roll container and gravity vector

//LED用
struct LEDFlag{
  int color = 0;
int light_time = 1000; //光るループ回数
int now_time = 0; //毎ループインクリメントしていく
}led_rainbow;
bool ledPatterns[10] = {true, false, false, false, false, false, false, false, false, false };
int nowState = 5;


volatile bool mpuInterrupt = false;     // indicates whether MPU interrupt pin has gone high

void setup() {
  Serial.begin(115200);
  Serial.setTimeout(300);
  Wire.begin();
  Wire.setClock(400000); // 400kHz I2C clock. Comment this line if having compilation difficulties

  pinMode(POWER_ON, OUTPUT);
  pinMode(CMD, OUTPUT);
  pinMode(12, OUTPUT);
  pinMode(7, OUTPUT);
  digitalWrite(CMD, HIGH);
  digitalWrite(POWER_ON, HIGH);//RN52の電源起動動作
  delay(1500);
  digitalWrite(POWER_ON, LOW);
  digitalWrite(7, HIGH);

  // initialize device
  Serial.println(F("Initializing I2C devices..."));
  mpu.initialize();
  pinMode(INTERRUPT_PIN, INPUT);

  // verify connection
  Serial.println(F("Testing device connections..."));
  Serial.println(mpu.testConnection() ? F("MPU6050 connection successful") : F("MPU6050 connection failed"));

  // load and configure the DMP
  Serial.println(F("Initializing DMP..."));
  devStatus = mpu.dmpInitialize();

  // supply your own gyro offsets here, scaled for min sensitivity
  mpu.setXGyroOffset(220);
  mpu.setYGyroOffset(76);
  mpu.setZGyroOffset(-85);
  mpu.setZAccelOffset(1788); // 1688 factory default for my test chip

  // make sure it worked (returns 0 if so)
  if (devStatus == 0) {
    // turn on the DMP, now that it's ready
    Serial.println(F("Enabling DMP..."));
    mpu.setDMPEnabled(true);

    // enable Arduino interrupt detection
    Serial.println(F("Enabling interrupt detection (Arduino external interrupt 0)..."));
    attachInterrupt(digitalPinToInterrupt(INTERRUPT_PIN), dmpDataReady, RISING);
    mpuIntStatus = mpu.getIntStatus();

    // set our DMP Ready flag so the main loop() function knows it's okay to use it
    Serial.println(F("DMP ready! Waiting for first interrupt..."));
    dmpReady = true;

    // get expected DMP packet size for later comparison
    packetSize = mpu.dmpGetFIFOPacketSize();
  } else {
    // ERROR!
    // 1 = initial memory load failed
    // 2 = DMP configuration updates failed
    // (if it's going to break, usually the code will be 1)
    Serial.print(F("DMP Initialization failed (code "));
    Serial.print(devStatus);
    Serial.println(F(")"));
  }

  // configure LED for output
  pinMode(LED_PIN, OUTPUT);


#if defined (__AVR_ATtiny85__)
  if (F_CPU == 16000000) clock_prescale_set(clock_div_1);
#endif
  strip.setBrightness(BRIGHTNESS);
  strip.begin();
  strip.show();
  pika_red();
}

VectorInt16 myRealaccel;
long int absolute_xy_accel;

void loop() {

  if (mpuInterrupt) {
    read_realaccel_form_mpu6050();
    absolute_xy_accel = (long int)aaReal.x * (long int)aaReal.x + (long int)aaReal.y * (long int)aaReal.y;
  }
  /////////////////////////////////////////CANPAI_DETECTION/////////////////////////////////

  if (CANPAI_THRESHOLD <= absolute_xy_accel) {
    /*Serial.print("kanpai!" + (String)counter);
      Serial.println("loopc" + (String)loopcounter);
      Serial.println(millis());
      Serial.println(time0);*/
    if (counter == 1 && millis() - time0 > CANPAI_WAITTIME) {
      Serial.println("kanpai!!!");
      time0 = millis();
    }
    ++counter;

  } else if (counterold != 1) {
    counter = 1;
    ++loopcounter;
  }
  counterold = counter;

  ///////////////////////////////////////////////////////////////////////////////////////////


  //////////////////////////////////////////IKKINOMI_DETECTION/////////////////////////////////

  /*if (ikki.enter_wait) {
    if (millis() - ikki.time0 > 1000) {
      Serial.println("waitting");
      Serial.println((long int)aaReal.z);
      ikki.time0 = millis();
    }
    if ((long int)aaReal.z > 4000) {
      ikki.enter_wait = false;
      ikki.enter_start = true;
      Serial.println("ikkistart");
    }
  }
  if (ikki.enter_start) {
    if (millis() - ikki.time0 > 1000) {
      Serial.println((long int)aaReal.z);
      ikki.time0 = millis();
    }
  }*/

  ledPatternCheck();
  //////////////////////////////////////////////////////////////////////////////////////////////////
}

void dmpDataReady() {//加速度を受信したとき、割り込み処理を行う
  mpuInterrupt = true;
}

void serialEvent() { //シリアル通信を受信したとき、割り込み処理を行う
  if (Serial.available()) {
    check_commands(read_command_from_serial());
  }
}

String read_command_from_serial() {
  String S_input;
  S_input = Serial.readString();
  char* p;
  unsigned long time0 = millis();
  for (p = &S_input[0]; *p != '\r' && *p != '\n' && *p != '\0'; p++) { //改行文字などを消す
    if (millis() - time0 > 1000) {
      Serial.println("ERROR:Serial_timeouted");
      break;
    }
  }
  *p = '\0';
  return &S_input[0];
}



void check_commands(String S_input) {
  if (S_input.startsWith("set,")) {
    String String_data = S_input.substring(4);
    write_data_to_rom(String_data);
    String data_from_rom = read_data_from_rom();
    Serial.println("saved");
    Serial.println("current," + data_from_rom);
  }
  else if (S_input.equals("get")) {
    String data_from_rom = read_data_from_rom();
    Serial.println("current," + data_from_rom);
  }
  else if (S_input.equals("red")) {
    pika_red();
    Serial.println("red");
    ledfalse(0);
  }
  else if (S_input.equals("green")) {
    pika_green();
    Serial.println("green");
    ledfalse(1);
  }
  else if (S_input.equals("blue")) {
    pika_blue();
    Serial.println("blue");
    ledfalse(2);
  }
  else if (S_input.equals("purple")) {
    pika_purple();
    Serial.println("purple");
    ledfalse(3);
  }
  else if (S_input.equals("yellow")) {
    pika_yellow();
    Serial.println("yellow");
    ledfalse(4);
  }
  else if (S_input.equals("pink")) {
    pika_pink();
    Serial.println("pink");
    ledfalse(5);
  }
  else if (S_input.equals("orange")) {
    pika_orange();
    Serial.println("orange");
    ledfalse(6);
  }
  else if (S_input.equals("lightblue")) {
    pika_yellow();
    Serial.println("yellow");
    ledfalse(7);
  }
  else if (S_input.equals("rainbow")) {
    pika_rainbow();
    Serial.println("rainbow");
    ledfalse(8);
  }
  else if (S_input.equals("ledoff")) {
    no_light();
    Serial.println("Led off");
    
  }
  else if (S_input.equals("ikki")) {
    Serial.println("ikki_mode");
    ikki.enter_wait = true;
  }
  else {
    Serial.println(S_input + " is not command");
  }
}

void ledPatternCheck(){
  if(ledPatterns[0]){
    //red 0,255,0
    ledfalse(0);
    pika_red();
  }else if(ledPatterns[1]){
    //green 255,0,0
    ledfalse(1);
    pika_green();
  }else if(ledPatterns[2]){
    //blue 0,0,255
    ledfalse(2);
    pika_blue();
  }else if(ledPatterns[3]){
    //purple 0,142,204
    ledfalse(3);
    pika_purple();
  }else if(ledPatterns[4]){
    //yellow 255 255 0
    ledfalse(4);
    pika_yellow();
  }else if(ledPatterns[5]){
    //pink 143 239 15
    ledfalse(5);
    pika_pink();
  }else if(ledPatterns[6]){
    //orange 183 255 76
    ledfalse(6);
    pika_orange();
  }else if(ledPatterns[7]){
    //light blue 135 25 22
    ledfalse(7);
    pika_light_blue();
  }else if(ledPatterns[8]){
    //rainbow
    ledfalse(8);
    pika_rainbow();
  }else if(ledPatterns[9]){
    //ledoff
    no_light();
    ledfalse(9);
  }else{
  }
}

void ledfalse(int index){
  //indexで指定したもの意外をすべてfalseにする
  int i=0;
  for(i=0; i<10; i++){
    if(i == index){
      ledPatterns[i] = true;
    }else{
      ledPatterns[i] = false;
    }
  }
}

void write_data_to_rom(String String_data) {
  byte Byte_data[30];
  String_data.getBytes(Byte_data, 30);
  int k;
  for (k = 0; k < 30 && Byte_data[k] != '\0'; k++) {
    EEPROM.write(k, Byte_data[k]);
  }
  EEPROM.write(k, '\0');
}

String read_data_from_rom(void) {
  String data_from_rom;
  for (int i = 0; i < 30 && EEPROM.read(i) != '\0' ; i++) {
    char c = EEPROM.read(i);
    data_from_rom = data_from_rom + String(c);
  }
  return data_from_rom;
}

void pika_red() {
  for (int i = 0; i < strip.numPixels(); i++) {
    strip.setPixelColor(i, strip.Color(0, 255, 0));
    strip.show();
  }
}

void pika_green() {
  for (int i = 0; i < strip.numPixels(); i++) {
    strip.setPixelColor(i, strip.Color(255, 0, 0));
    strip.show();
  }
}

void pika_blue() {
  int i;
  for (i = 0; i < strip.numPixels(); i++) {
    strip.setPixelColor(i, strip.Color(0, 0, 255));
  }
  strip.show();
}

void pika_purple(){
  int i;
  for(i=0; i<strip.numPixels(); i++){
    strip.setPixelColor(i, strip.Color(0,142,204));
  }
  strip.show();
}

void pika_yellow(){
  int i;
  for(i=0; i<strip.numPixels(); i++){
    strip.setPixelColor(i, strip.Color(255,255,0));
  }
  strip.show();
}

void pika_pink(){
  int i;
  for(i=0; i<strip.numPixels(); i++){
    strip.setPixelColor(i, strip.Color(143, 239, 15));
  }
  strip.show();
}

void pika_orange(){
  int i;
  for(i=0; i<strip.numPixels(); i++){
    strip.setPixelColor(i, strip.Color(183,255,76));
  }
  strip.show();
}

void pika_light_blue(){
  int i;
  for(i=0; i<strip.numPixels(); i++){
    strip.setPixelColor(i, strip.Color(135,25,22));
  }
  strip.show();
}



void ziwaziwa() {
  for (int j = 0; j < 256; j++) {
    for (int i = 0; i < strip.numPixels(); i++) {
      //strip.setPixel(i, Wheel(mycolor & 255));
    }
    strip.show();
  }
}

void no_light() {
  for (int i = 0; i < strip.numPixels(); i++) {
    strip.setPixelColor(i, strip.Color(0, 0, 0));
    strip.show();
  }
}

uint32_t Wheel(byte WheelPos) {
  WheelPos = 255 - WheelPos;
  if (WheelPos < 85) {
    return strip.Color(0, 255 - WheelPos * 3, WheelPos * 3);
  }
  if (WheelPos < 170) {
    WheelPos -= 85;
    return strip.Color(WheelPos * 3, 0, 255 - WheelPos * 3);
  }
  WheelPos -= 170;
  return strip.Color(255 - WheelPos * 3, WheelPos * 3,0);
}

void pika_rainbow(){
  int i;
  if(led_rainbow.now_time > led_rainbow.light_time){
    led_rainbow.now_time = 0;
    led_rainbow.color++;
    for(i=0; i<strip.numPixels(); i++){
      strip.setPixelColor(i, Wheel(led_rainbow.color & 255));
    }
    strip.show();
  }else{
    led_rainbow.now_time++;
  }
}

void read_realaccel_form_mpu6050(void) {
  mpuInterrupt = false;

  mpuIntStatus = mpu.getIntStatus();

  fifoCount = mpu.getFIFOCount();

  // check for overflow
  if ((mpuIntStatus & 0x10) || fifoCount == 1024) {
    // reset so we can continue cleanly
    mpu.resetFIFO();
    //Serial.println(F("FIFO overflow!"));

    // otherwise, check for DMP data ready interrupt (this should happen frequently)
  } else if (mpuIntStatus & 0x02) {


    // wait for correct available data length, should be a VERY short wait
    while (fifoCount < packetSize) fifoCount = mpu.getFIFOCount();

    // read a packet from FIFO
    mpu.getFIFOBytes(fifoBuffer, packetSize);

    // display real acceleration, adjusted to remove gravity
    mpu.dmpGetQuaternion(&q, fifoBuffer);
    mpu.dmpGetAccel(&aa, fifoBuffer);
    mpu.dmpGetGravity(&gravity, &q);
    mpu.dmpGetLinearAccel(&aaReal, &aa, &gravity);

    blinkState = !blinkState;
    digitalWrite(LED_PIN, blinkState);
  }
}
