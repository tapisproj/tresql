package code
package snippet

import net.liftweb.http.js._
import net.liftweb.http._
import net.liftweb.util.Helpers._
import net.liftweb._

object AjaxQuery {
  def render = {
    var queryString = ""
    var par1 = ""
    var par2 = ""
    var par3 = ""
    var par4 = ""
    var par5 = ""
    var resultString = ""

    def process(): JsCmd = {
      S.notice("Query: " + queryString)
      S.notice("Par1: " + par1)
      S.notice("Par2: " + par2)
      S.notice("Par3: " + par3)
      S.notice("Par4: " + par4)
      S.notice("Par5: " + par5)
      try {
        resultString = //
          QueryServer.json(queryString, List(par1, par2, par3, par4, par5))
        S.notice("RESULT: " + resultString)
      } catch {
        case e: Exception => S.notice("ERROR: " + e.toString)
      }
    }

    "#result *" #> resultString &
      "name=query" #> SHtml.textarea(queryString, queryString = _) &
      "name=par1" #> SHtml.text(par1, par1 = _) &
      "name=par2" #> SHtml.text(par2, par2 = _) &
      "name=par3" #> SHtml.text(par3, par3 = _) &
      "name=par4" #> SHtml.text(par4, par4 = _) &
      "name=par5" #> SHtml.text(par5, par5 = _) &
      "#hideme" #> SHtml.hidden(process)
  }
}
