#include <SPI.h>
#include <Adafruit_GFX.h>
#include <LCD_I2C.h>

//подключение рефлекторного фото датчика brown=+5, blue=grnd, black=signal
#define Photo_sensor A1  //назначаем фотодатчик на ногу ардуины//
unsigned int resistorValue = 0;

boolean mode = true;
boolean reset = true;
boolean blocked = false;
boolean startstop = false;
boolean displayed = false;

unsigned long timer, t, tstart, sleep, sleep2;  //только так и не иначе будут страшные глюки
int m, s, ms;
int mBst, sBst, msBst;
String strms, strs, strm, disptime, disptimeBest, testmsg;

String svms = "00";  // сотые доли секунд
String svs = "00";   // секунды
String svm = "00";   // минуты

unsigned long start, finished, elapsed;
boolean r = false;

// подключение LCD
LCD_I2C lcd(0x27, 16, 2);  // Default address of most PCF8574 modules, change according

void setup() {
  Serial.begin(9600);  //стыкуемся с монитором порта


  pinMode(Photo_sensor, INPUT_PULLUP);  // включаем внутренний резистор для фотобарьера
  //pinMode(5,INPUT); //Сигнальный контакт — контакт 5 (Pin 5)
  lcd.begin();
  lcd.clear();
  lcd.backlight();
  lcd.setCursor(4, 0);
  lcd.print("End setup");
  lcd.clear();
  Serial.println("End setup");
}

void displayNumber(String number) {
  lcd.setCursor(0, 1);  // Or setting the cursor in the desired position.
  lcd.print("Current:" + number);
}

void displayNumberBest(String number) {
  lcd.setCursor(0, 0);
  lcd.print("Best Tm:" + number);
}

void displayMessage(String message) {
  lcd.setCursor(0, 0);
  message.trim();
  lcd.print("                ");
  lcd.setCursor(0, 0);
  lcd.print(message);
}

void loop() {
  // время
  t = millis();                           //считываем время работы ардуинки
  timer = (t - tstart);                   //берем разницу полного времени работы и времени нажатия кнопки и получаем время, которое прошло после нажатия
  s = floor((timer - m * 60000) / 1000);  //высчитываем секунды
  m = floor(timer / 60000);               //высчитываем минуты
  ms = timer % 1000 / 10;                 //высчитываем сотые доли секунды

  //преобразуем в удобный вывод
  //Если число меньше 10, то в нем 1 символ, поэтому, преобразуем в текст и добавляем спереди символ "0"
  if (ms < 10) strms = "0" + String(ms);
  else strms = String(ms);
  if (s < 10) strs = "0" + String(s);
  else strs = String(s);
  if (m < 10) strm = "0" + String(m);
  else strm = String(m);

  // тут мы будем показывать то, что находится на паузе
  if (mode == 1)  //если програма в состоянии стоп/пауза mode=1
  {
    //  Выводятся значения, сохраненные в переменной на момент нажатия на паузу
    disptime = svm + "." + svs + "." + svms;  //формируем строку вывода
    displayNumber(disptime);                  //вывод на дисплей 2-й строки
    tstart = t;                               //тут сохраняем время нажатия кнопки, дабы с помощью разницы от общего времени работы вычислить сколько работает секундомер
    if (sBst == 0) {
      disptimeBest = svm + "." + svs + "." + svms;
      sBst = s;
      mBst = m;
      msBst = ms;
      // displayNumberBest(disptimeBest);
    } else {
      if (svm.toInt() <= mBst) {
        if (svs.toInt() <= sBst) {
          if (svs.toInt() < sBst) {
            disptimeBest = svm + "." + svs + "." + svms;
            // displayNumberBest(disptimeBest);
            mBst = svm.toInt();
            sBst = svs.toInt();
            msBst = svms.toInt();
          } else {
            if (svms.toInt() < msBst) {
              disptimeBest = svm + "." + svs + "." + svms;
              // displayNumberBest(disptimeBest);
              mBst = svm.toInt();
              sBst = svs.toInt();
              msBst = svms.toInt();
            }
          }
        }
      }
    }
    if (!displayed) displayNumberBest(disptimeBest);
    m = s = ms = 0;  //сбрасываем значения на 0, они нам больше не нужны ибо мы их сохранили в mode=0
  }

  //а тут динамическое изображение циферок
  if (mode == 0)  //если программа находится в состоянии работы mode=0
  {
    // выводим динамически циферки
    disptime = svm + "." + svs + "." + svms;  //формируем строку вывода
    displayNumber(disptime);                  //вывод на дисплей
    svms = strms;                             //сохраняем в переменную милисекунды ибо неожиданно может быть нажата кнопка остановки
    svs = strs;                               //так же сохраняем секунды
    svm = strm;                               //и минуты
  }

  // обработка кнопки старт/стоп
  resistorValue = analogRead(A1);
  Serial.println(resistorValue);
  if (resistorValue < 500) {  //если на барьере старт/стоп появился логический 0
    if (t - sleep > 1000) {   //нельзя брать сигнал с барьера чаще чем 1 раз в 1 секунду
      sleep = t;              //так же это сделано чтобы избежать "дребезга кнопок" используя кнопки без подтяжки, всё это делается программно
      mode = !mode;           //инверсия старт/стоп
      displayed = false;
    }
  }

    // вывод пришедшего сообщения в первой строке
  if (Serial.available() > 0) {
    testmsg = Serial.readStringUntil('\n');
    displayMessage(testmsg);
    displayed = true;
  }
}
