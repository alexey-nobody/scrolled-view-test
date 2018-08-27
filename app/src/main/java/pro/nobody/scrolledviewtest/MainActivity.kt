/*
Copyright © 2018 Alexey Lepskii | nobodypro.ru
All rights reserved.
*/
package pro.nobody.scrolledviewtest

import android.app.Activity
import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ScrollView
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import java.util.*
import kotlinx.coroutines.experimental.launch


class MainActivity : AppCompatActivity() {

  private val debugTag = "DEBUGSCROLL" // тег для отладочной информации

  private var wordsInMin = 120 // скорость чтения слов в минуту

  private var pause = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    speedEt.setText(wordsInMin.toString())
  }

  override fun onResume() {
    super.onResume()

    speedEt.setOnFocusChangeListener { v, hasFocus ->
      pause = hasFocus
    }

    //btn set speed setting
    setSpeedButton.setOnClickListener {
      hideKeyboard()
      speedEt.clearFocus()
      wordsInMin = speedEt.text.toString().toInt()
      Snackbar.make(contentLayout, "OK!", Snackbar.LENGTH_LONG).show()
    }

    playerStart()
  }

  //метод для скрытия клавиатуры
  private fun Activity.hideKeyboard() {
    hideKeyboard(if (currentFocus == null) View(this) else currentFocus)
  }

  private fun Context.hideKeyboard(view: View) {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
  }

  //метод для получения текста с центральной строки в ScrollView
  private fun getTextFromCenterLine(textView: TextView?, scrollView: ScrollView): String {
    val tvLayout = textView?.layout
    if (tvLayout != null) {
      //определяем начало центральной области
      val centerLineStart = scrollView.scrollY + (scrollView.height / 2)
      //получаем какая строчка находится в середине экрана
      val screenCenterLine = tvLayout.getLineForVertical(centerLineStart)
      //получаем ее начало и конец в символах
      val textOffsetStart = tvLayout.getLineStart(screenCenterLine)
      val textOffsetEnd = tvLayout.getLineEnd(screenCenterLine)
      //вырезаем центральную строчку
      return tvLayout.text.substring(textOffsetStart, textOffsetEnd)
    }

    return ""
  }

  //метод для получения количество слов на экране
  private fun getNumWordsOnText(text: String): Int {
    val st = StringTokenizer(text)

    Log.d(debugTag, "В тексте на экране ${st.countTokens()} слов(о)")

    return st.countTokens()
  }

  //метод для получения количества строк которые поместятся на экране
  private fun getNumLineOnScreen(screenHeight: Int, lineHeight: Int): Int {
    return (screenHeight / lineHeight) + 1
  }

  //метод для добавления блока текста в начало и в конец
  private fun appendTextToStartEnd(text: String, appendText: String): String {
    return appendText + text + appendText
  }

  //метод для подготовки текста в textView для прокрутки
  private fun prepareTextForScrolling(text: String, countLine: Int): String {
    var space = ""
    for (i in 1..countLine) {
      space += "\n"
    }
    return appendTextToStartEnd(text, space)
  }

  //загрузка контента
  private suspend fun contentLoader() = async {
    val job = launch(UI) {
      val screenLines = getNumLineOnScreen(scrolledView.height, contentTextView.lineHeight) // получаем количество линий на экране
      contentTextView.text = prepareTextForScrolling(contentTextView.text.toString(), screenLines) // подготавливаем текст для прокрутки
    }
    job.join()
  }

  private fun playerStart() = launch(UI) {
    //расчитываем необходимые параметры
    val delayOneWord = (60F / wordsInMin) * 1000 // задержка на одно слово

    var delayOnAllText = 0F//переменная для расчета времени в мс которое потребовалось на прокрутку текста с заданой скоростью

    contentLoader()//загружаем контент

    delay(2000)

    val lineHeight: Float = contentTextView.height.toFloat() / contentTextView.lineCount

    player@ while (true) {
      //check pause flag
      if (!pause) {
        val textOnScreenCenterLine = getTextFromCenterLine(contentTextView, scrolledView)
        val numWordsOnCenterLine = getNumWordsOnText(textOnScreenCenterLine)

        if (numWordsOnCenterLine > 0) {

          val delay = (numWordsOnCenterLine * delayOneWord) / lineHeight

          for (i in 1..lineHeight.toInt()) {
            scrolledView.smoothScrollTo(0, scrolledView.scrollY + 1)
            delayOnAllText += delay//считаем время в мс которое потребовалось на текст
            delay(delay.toInt())
          }

        } else {
          scrolledView.smoothScrollTo(0, scrolledView.scrollY + 4)
          delay(10)
        }
      } else {
        delay(1000)
      }

      //check end of scroll
      if (scrolledView.getChildAt(0).bottom == (scrolledView.height + scrolledView.scrollY)) {
        Snackbar.make(contentLayout, "Read time - ${delayOnAllText / 1000} sec", Snackbar.LENGTH_LONG).show()
        delayOnAllText = 0F
        scrolledView.smoothScrollTo(0,0)
      }
    }
  }
}