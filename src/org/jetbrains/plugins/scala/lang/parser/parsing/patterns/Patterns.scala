package org.jetbrains.plugins.scala.lang.parser.parsing.patterns
/**
* @author Ilya Sergey
*/
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

object SimplePattern {
  /*
  SimplePattern ::=   _�
                      | varid
                      | Literal
                      | StableId [ ( [Patterns] ) ]
                      | ( [Pattern] )
                      | XmlPattern
  */

  def parse(builder: PsiBuilder): ScalaElementType = {
    var spMarker = builder.mark()



    // Process ")" symbol
    def closeParent = {
      if (ScalaTokenTypes.tRPARENTHESIS.equals(builder.getTokenType)){
        ParserUtils.eatElement(builder, ScalaTokenTypes.tRPARENTHESIS)
        spMarker.done(ScalaElementTypes.SIMPLE_PATTERN)
        ScalaElementTypes.SIMPLE_PATTERN
      }
      else {
        spMarker.error(") expected")
        ScalaElementTypes.SIMPLE_PATTERN
      }
    }



    def argsParse: ScalaElementType = {
      var argsMarker = builder.mark()

      // Process ")" symbol
      def closeParent: ScalaElementType = {
        ParserUtils.eatElement(builder, ScalaTokenTypes.tRPARENTHESIS)
        argsMarker.done(ScalaElementTypes.PATTERNS)
        ScalaElementTypes.PATTERNS
      }

      if (ScalaTokenTypes.tLPARENTHESIS.equals(builder getTokenType)){
        ParserUtils.eatElement(builder, ScalaTokenTypes.tLPARENTHESIS)
        if (ScalaTokenTypes.tRPARENTHESIS.eq(builder getTokenType)) {
          closeParent
        } else {
          var res = Patterns.parse(builder)
          if (res.equals(ScalaElementTypes.PATTERNS)) {
            // LOOK!!! ParserUtils.rollForward(builder)
            if (ScalaTokenTypes.tRPARENTHESIS.eq(builder getTokenType)) {
              closeParent
            } else {
              builder.error(") expected")
              argsMarker.done(ScalaElementTypes.PATTERNS)
              ScalaElementTypes.PATTERNS
            }
          } else {
            builder.error("Wrong patterns")
            argsMarker.done(ScalaElementTypes.PATTERNS)
            ScalaElementTypes.PATTERNS
          }
        }
      } else {
        argsMarker.rollbackTo()
        ScalaElementTypes.WRONGWAY
      }
    }

    def parseStableId: ScalaElementType = {
      var result = StableId.parse(builder)
      if (ScalaElementTypes.STABLE_ID.equals(result) || ScalaElementTypes.STABLE_ID_ID.equals(result)) {
        if (ScalaTokenTypes.tLPARENTHESIS.equals(builder.getTokenType)){
          argsParse
          spMarker.done(ScalaElementTypes.SIMPLE_PATTERN1)
          ScalaElementTypes.SIMPLE_PATTERN
        } else {
          spMarker.drop()
          ScalaElementTypes.SIMPLE_PATTERN
        }
      } else {
        spMarker.drop()
        builder.error("Wrong pattern!")
        ScalaElementTypes.SIMPLE_PATTERN
      }
    }

    //  _�
    if (builder.getTokenType == ScalaTokenTypes.tUNDER) {
      val uMarker = builder.mark()
      ParserUtils.eatElement(builder, ScalaTokenTypes.tUNDER)
      uMarker.done(ScalaElementTypes.WILD_PATTERN)
      spMarker.drop()
      ScalaElementTypes.SIMPLE_PATTERN
      // ( [Pattern] )
    } /*if (ScalaTokenTypes.tLPARENTHESIS.eq(builder getTokenType)){
      var um = builder.mark()
      ParserUtils.eatElement(builder, ScalaTokenTypes.tLPARENTHESIS)
      // LOOK!!! ParserUtils.rollForward(builder)
      if (ScalaTokenTypes.tRPARENTHESIS.eq(builder getTokenType)) {
        ParserUtils.eatElement(builder, ScalaTokenTypes.tRPARENTHESIS)
        um.done(ScalaElementTypes.UNIT)
        spMarker.drop()
        ScalaElementTypes.SIMPLE_PATTERN
      } else {
        um.drop()
        var res = Pattern.parse(builder)
        if (ScalaElementTypes.PATTERN.equals(res)) {
          if (ScalaTokenTypes.tRPARENTHESIS.equals(builder getTokenType)) {
            closeParent
          } else {
            builder.error(") expected")
            spMarker.drop()
            ScalaElementTypes.SIMPLE_PATTERN
          }
        } else {
          builder.error("Wrong patterns")
          spMarker.drop()
          ScalaElementTypes.SIMPLE_PATTERN
        }
      }
      //  ( [Pattern , [Patterns [,]]] )
    } else */
      else if (ScalaTokenTypes.tLPARENTHESIS.equals(builder.getTokenType)) {
      val uMarker = builder.mark()
      ParserUtils.eatElement(builder, ScalaTokenTypes.tLPARENTHESIS)
      if (ScalaTokenTypes.tRPARENTHESIS.equals(builder.getTokenType)){
        ParserUtils.eatElement(builder, ScalaTokenTypes.tRPARENTHESIS)
        uMarker.done(ScalaElementTypes.UNIT)
        spMarker.drop()
        ScalaElementTypes.SIMPLE_PATTERN
      } else {
        uMarker.drop()
        var res = Pattern.parse(builder)
        if (! ScalaElementTypes.WRONGWAY.equals(res)) {
          if (ScalaTokenTypes.tCOMMA.equals(builder.getTokenType)){
            ParserUtils.eatElement(builder, ScalaTokenTypes.tCOMMA)
            if (ScalaTokenTypes.tRPARENTHESIS.equals(builder.getTokenType)){
              closeParent
            } else {
              val res1 = Patterns.parse(builder)
              if (! ScalaElementTypes.WRONGWAY.equals(res1)) {
                if (ScalaTokenTypes.tCOMMA.equals(builder.getTokenType)){
                  ParserUtils.eatElement(builder, ScalaTokenTypes.tCOMMA)
                }
                closeParent
              } else closeParent
            }
          } else {
            builder.error(", expected")
            spMarker.done(ScalaElementTypes.SIMPLE_PATTERN)
            ScalaElementTypes.SIMPLE_PATTERN
          }
        } else {
          spMarker.error("Argument type pattern expected")
          ScalaElementTypes.SIMPLE_PATTERN
        }
      }
    } // Literal
    else if (Literal.parse(builder)) {
      spMarker.done(ScalaElementTypes.LITERAL_PATTERN)
      ScalaElementTypes.SIMPLE_PATTERN
      /*  | varid
| StableId [ ( [Patterns] ) ] */
    } else if (builder.getTokenType == ScalaTokenTypes.tIDENTIFIER){
      if (builder.getTokenText.substring(0, 1).toLowerCase ==
      builder.getTokenText.substring(0, 1)) {// if variable id
        val vm = builder.mark()
        ParserUtils.eatElement(builder, ScalaTokenTypes.tIDENTIFIER)
        vm.done(ScalaElementTypes.REFERENCE_PATTERN)
        if (! builder.getTokenType.equals(ScalaTokenTypes.tLPARENTHESIS) &&
        ! builder.getTokenType.equals(ScalaTokenTypes.tDOT)) {
          spMarker.drop()
          ScalaElementTypes.SIMPLE_PATTERN
        } else {
          spMarker.rollbackTo()
          spMarker = builder.mark()
          parseStableId
        }
      } else { parseStableId }
    } else {
      //spMarker.drop()
      spMarker.rollbackTo()
      ScalaElementTypes.WRONGWAY
    }
  }
}


object Pattern3 extends InfixTemplate(ScalaElementTypes.PATTERN3, SimplePattern.parse, SimplePattern.parse)

class Pattern2 {  
  /*
    Pattern2 ::=   varid [@ Pattern3]
                 | Pattern3
  */

  def parse(builder: PsiBuilder): ScalaElementType = {
    var rbMarker = builder.mark()

    val p2m = builder.mark()

    def parsePattern3: ScalaElementType = {
      var result = Pattern3.parse(builder)
      if (ScalaElementTypes.PATTERN3.equals(result)) {
        ScalaElementTypes.PATTERN2
      } else {
        ScalaElementTypes.WRONGWAY
      }
    }

    if (builder.getTokenType == ScalaTokenTypes.tIDENTIFIER &&
    (builder.getTokenText.substring(0, 1).toLowerCase ==
    builder.getTokenText.substring(0, 1))) {
      val vm = builder.mark()
      ParserUtils.eatElement(builder, ScalaTokenTypes.tIDENTIFIER)
      vm.done(ScalaElementTypes.REFERENCE)
      if (builder.getTokenType == ScalaTokenTypes.tAT) {
        ParserUtils.eatElement(builder, ScalaTokenTypes.tAT)
        rbMarker.drop()
        var res =
          if (BNF.firstPattern2.contains(builder.getTokenType))
            Pattern3.parse(builder)
          else
            ScalaElementTypes.WRONGWAY
        if (ScalaElementTypes.PATTERN3.equals(res)){
          p2m.done(ScalaElementTypes.PATTERN2)
          ScalaElementTypes.PATTERN2
        } else {
          builder.error("Wrong simple pattern(s)")
          p2m.done(ScalaElementTypes.PATTERN2)
          ScalaElementTypes.WRONGWAY
        }
      } else {
        p2m.drop()
        rbMarker.rollbackTo()
        rbMarker = builder.mark()
        var result = parsePattern3
        if (ScalaElementTypes.PATTERN2.equals(result)) {
          rbMarker.drop()
          result
        }else {
          rbMarker.rollbackTo()
          val vm = builder.mark()
          ParserUtils.eatElement(builder, ScalaTokenTypes.tIDENTIFIER)
          vm.done(ScalaElementTypes.REFERENCE)
          ScalaElementTypes.PATTERN2
        }
      }
    } else {
      p2m.drop()
      rbMarker.drop()
      parsePattern3
    }
  }
}


/*object Pattern1 {
  /*
  Pattern1 ::=    varid : TypePattern
                | _� : TypePattern
                | Pattern2
  */

  def parse(builder: PsiBuilder): ScalaElementType = {
    var p1Marker = builder.mark()

    def parsePattern2: ScalaElementType = {
      p1Marker.rollbackTo()
      p1Marker = builder.mark()
      var result = (new Pattern2()).parse(builder)
      if (ScalaElementTypes.PATTERN2.equals(result)) {
        p1Marker.drop
        //p1Marker.done(ScalaElementTypes.PATTERN1)
        ScalaElementTypes.PATTERN1
      }
      else {
        p1Marker.drop()
        ScalaElementTypes.WRONGWAY
      }
    }
    //  _�
    if (builder.getTokenType == ScalaTokenTypes.tUNDER) {
      ParserUtils.eatElement(builder, ScalaTokenTypes.tUNDER)
      if (builder.getTokenType == ScalaTokenTypes.tCOLON) {
        ParserUtils.eatElement(builder, ScalaTokenTypes.tCOLON)
        var res = Type.parse(builder)
        if (res.equals(ScalaElementTypes.TYPE_PATTERN)) {
          p1Marker.done(ScalaElementTypes.TYPE_PATTERN)
          ScalaElementTypes.PATTERN1
        } else {
          builder.error("Type declaration expected")
          p1Marker.done(ScalaElementTypes.TYPE_PATTERN)
          ScalaElementTypes.PATTERN1
        }
      } else {
        parsePattern2
      }
    } else if (builder.getTokenType == ScalaTokenTypes.tIDENTIFIER &&
    (builder.getTokenText.substring(0, 1).toLowerCase ==
    builder.getTokenText.substring(0, 1))) {
      val vm = builder.mark()
      ParserUtils.eatElement(builder, ScalaTokenTypes.tIDENTIFIER)
      vm.done(ScalaElementTypes.REFERENCE)
      if (builder.getTokenType == ScalaTokenTypes.tCOLON) {
        ParserUtils.eatElement(builder, ScalaTokenTypes.tCOLON)
        var res = Type.parse(builder)
        if (res.equals(ScalaElementTypes.TYPE_PATTERN)) {
          p1Marker.done(ScalaElementTypes.TYPE_PATTERN)
          ScalaElementTypes.PATTERN1
        } else {
          p1Marker.done(ScalaElementTypes.TYPE_PATTERN)
          builder.error("Type declaration expected")
          ScalaElementTypes.PATTERN1
        }
      } else {
        parsePattern2
      }
    } else {
      parsePattern2
    }

  }
}*/

/*object Pattern {
  /*                               
    Pattern ::= Pattern1 { | Pattern1 }
  */

  def parse(builder: PsiBuilder): ScalaElementType = {
    val pMarker = builder.mark()

    def subParse: ScalaElementType = {
      var res = Pattern1.parse(builder)
      if (res.equals(ScalaElementTypes.PATTERN1)){
        // LOOK!!! ParserUtils.rollForward(builder)
        if (builder.getTokenType != null &&
        builder.getTokenText.equals("|")) {
          ParserUtils.eatElement(builder, ScalaTokenTypes.tOR)
          // LOOK!!! ParserUtils.rollForward(builder)
          subParse
        } else {
          pMarker.done(ScalaElementTypes.PATTERN)
          ScalaElementTypes.PATTERN
        }
      } else {
        builder error "Pattern expected"
        pMarker.done(ScalaElementTypes.PATTERN)
        ScalaElementTypes.PATTERN
      }
    }

    var result = Pattern1.parse(builder)
    if (result.equals(ScalaElementTypes.PATTERN1)) {
      // LOOK!!! ParserUtils.rollForward(builder)
      if (builder.getTokenType != null &&
      builder.getTokenText.equals("|")) {
        ParserUtils.eatElement(builder, ScalaTokenTypes.tOR)
        // LOOK!!! ParserUtils.rollForward(builder)
        subParse
      } else {
        //pMarker.done(ScalaElementTypes.PATTERN)
        pMarker.drop
        ScalaElementTypes.PATTERN
      }
    } else {
      pMarker.rollbackTo()
      ScalaElementTypes.WRONGWAY
    }
  }
}*/

object Patterns {
  /*
    Patterns ::=    Pattern [, Patterns]
                  | _� *
  */

  def parse(builder: PsiBuilder): ScalaElementType = {
    var psMarker = builder.mark()

    def subParse: ScalaElementType = {
      // LOOK!!! ParserUtils.rollForward(builder)
      if (ScalaTokenTypes.tCOMMA.equals(builder.getTokenType)) {
        val rbMarker = builder.mark()
        ParserUtils.eatElement(builder, ScalaTokenTypes.tCOMMA)
        val res = Pattern.parse(builder)
        if (! ScalaElementTypes.WRONGWAY.equals(builder.getTokenType)){
          rbMarker.drop()
          subParse
        } else {
          rbMarker.rollbackTo()
          psMarker.drop()
          ScalaElementTypes.PATTERNS
        }
      } else {
        psMarker.drop()
        ScalaElementTypes.PATTERNS
      }
    }

    def parseSequence: ScalaElementType = {

      var rm = builder.mark()
      val varid = if (! builder.eof) builder.getTokenType else null
      val text = if (! builder.eof) builder.getTokenText else null
      builder.advanceLexer
      val at = if (! builder.eof) builder.getTokenType else null
      builder.advanceLexer
      val under = if (! builder.eof) builder.getTokenType else null
      builder.advanceLexer
      val star = if (! builder.eof) builder.getTokenText else null
      rm.rollbackTo()

      if (ScalaTokenTypes.tIDENTIFIER.equals(varid) &&
      text.substring(0, 1).toLowerCase == text.substring(0, 1) &&
      ScalaTokenTypes.tAT.equals(at) &&
      ScalaTokenTypes.tUNDER.equals(under) &&
      "*".equals(star)){
        ParserUtils.eatElement(builder, ScalaTokenTypes.tIDENTIFIER); builder.getTokenType
        ParserUtils.eatElement(builder, ScalaTokenTypes.tAT); builder.getTokenType
        val uMarker = builder.mark()
        ParserUtils.eatElement(builder, ScalaTokenTypes.tUNDER); builder.getTokenType
        ParserUtils.eatElement(builder, ScalaTokenTypes.tSTAR); builder.getTokenType
        uMarker.done(ScalaElementTypes.WILD_PATTERN)
        psMarker.drop()
        ScalaElementTypes.PATTERNS
      } else {
        var res = Pattern parse builder
        if (ScalaElementTypes.PATTERN.equals(res)) {
          subParse
        } else {
          builder.error("Wrong parser sequence")
          psMarker.drop()
          ScalaElementTypes.WRONGWAY
        }
      }
    }

    if (ScalaTokenTypes.tUNDER.equals(builder.getTokenType)){
      ParserUtils.eatElement(builder, ScalaTokenTypes.tUNDER)
      if ("*"==builder.getTokenText){
        ParserUtils.eatElement(builder, ScalaTokenTypes.tSTAR)
        psMarker.done(ScalaElementTypes.WILD_PATTERN)
        ScalaElementTypes.PATTERNS
      } else {
        psMarker.rollbackTo()
        psMarker = builder.mark()
        parseSequence
      }
    } else parseSequence
  }
}



/*object CaseClause {

  def parse(builder: PsiBuilder): ScalaElementType = {
    val caseMarker = builder.mark()

    def negative(st: String): ScalaElementType = {
      builder.error(st)
      caseMarker.done(ScalaElementTypes.CASE_CLAUSE)
      ScalaElementTypes.CASE_CLAUSE
    }

    if (ScalaTokenTypes.kCASE.equals(builder.getTokenType)){
      ParserUtils.eatElement(builder, ScalaTokenTypes.kCASE)
      var res = Pattern parse builder
      if (res.equals(ScalaElementTypes.PATTERN)) {
        var flag = true
        if (ScalaTokenTypes.kIF.equals(builder.getTokenType)) {
          ParserUtils.eatElement(builder, ScalaTokenTypes.kCASE)
          var res2 = PostfixExpr parse builder
          if (ScalaElementTypes.WRONGWAY.equals(res2))  {
            builder.error("Wrong expression")
            flag = false
          }
        }
        if (ScalaTokenTypes.tFUNTYPE == builder.getTokenType) {
          ParserUtils.eatElement(builder, ScalaTokenTypes.tFUNTYPE)
          var res1 = Block.parse(builder, false)
          if (flag && res1.equals(ScalaElementTypes.BLOCK)) {
            caseMarker.done(ScalaElementTypes.CASE_CLAUSE)
            ScalaElementTypes.CASE_CLAUSE
          } else {
            negative("Wrong expression!")
          }
        } else {
          negative("=> expected")
        }
      } else {
        negative("Pattern expected")
      }
    } else {
      caseMarker.drop()
      ScalaElementTypes.WRONGWAY
    }
  }
}*/

/*object CaseClauses {
  /*
    CaseClauses ::= CaseClause { CaseClause }
  */
  def parse(builder: PsiBuilder): ScalaElementType = {

    val ccMarker = builder.mark()
    var result = CaseClause.parse(builder)
    if (ScalaElementTypes.CASE_CLAUSE.equals(result)) {
      while (! builder.eof && ScalaElementTypes.CASE_CLAUSE.equals(result)){
       /* while (! builder.eof && (ScalaTokenTypes.tLINE_TERMINATOR.equals(builder.getTokenType)
        || ScalaTokenTypes.tWHITE_SPACE_IN_LINE.equals(builder.getTokenType)))
        {
          if (ScalaTokenTypes.tLINE_TERMINATOR.equals(builder.getTokenType))
            ParserUtils.eatElement(builder, ScalaTokenTypes.tLINE_TERMINATOR)
          else if (ScalaTokenTypes.tWHITE_SPACE_IN_LINE.equals(builder.getTokenType))
            ParserUtils.eatElement(builder, ScalaTokenTypes.tWHITE_SPACE_IN_LINE)
        }
        if (! builder.eof)  */
        result = CaseClause.parse(builder)
      }
    }
    ccMarker.done(ScalaElementTypes.CASE_CLAUSES)
    ScalaElementTypes.CASE_CLAUSES
  }
}*/

