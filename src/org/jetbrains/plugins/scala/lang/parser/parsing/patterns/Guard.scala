package org.jetbrains.plugins.scala.lang.parser.parsing.patterns

import com.intellij.lang.PsiBuilder, org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypes
import org.jetbrains.plugins.scala.lang.parser.ScalaElementTypes
import org.jetbrains.plugins.scala.lang.lexer.ScalaElementType
import org.jetbrains.plugins.scala.lang.parser.bnf.BNF
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.tree.IElementType
import org.jetbrains.plugins.scala.lang.parser.util.ParserUtils
import org.jetbrains.plugins.scala.lang.parser.parsing.types._
import org.jetbrains.plugins.scala.lang.parser.parsing.top.template._
import org.jetbrains.plugins.scala.lang.parser.parsing.expressions._
import org.jetbrains.plugins.scala.lang.parser.bnf._

/** 
* Created by IntelliJ IDEA.
* User: Alexander.Podkhalyuz
* Date: 28.02.2008
* Time: 17:35:01
* To change this template use File | Settings | File Templates.
*/

object Guard {
  def parse(builder: PsiBuilder): Boolean = {
    val guardMarker = builder.mark
    builder.getTokenType match {
      case ScalaTokenTypes.kIF => {
        builder.advanceLexer //Ate if
      }
      case _ => {
        guardMarker.drop
        return false
      }
    }
    if (PostfixExpr.parse(builder) == ScalaElementTypes.WRONGWAY) {
      builder error ScalaBundle.message("wrong.postfix.expression", new Array[Object](0))
    }
    guardMarker.done(ScalaElementTypes.GUARD)
    return true
  }
}