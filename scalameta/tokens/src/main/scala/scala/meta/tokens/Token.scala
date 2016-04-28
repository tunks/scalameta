package scala.meta
package tokens

import scala.meta.internal.tokens
import scala.meta.internal.tokens._
import scala.meta.inputs._
import scala.meta.classifiers._
import scala.meta.prettyprinters._
import scala.meta.internal.prettyprinters._

// NOTE: `start` and `end` are String.substring-style,
// i.e. `start` is inclusive and `end` is not.
// Therefore Token.end can point to the last character plus one.
// Btw, Token.start can also point to the last character plus one if it's an EOF token.
@root trait Token {
  def content: Content
  def dialect: Dialect

  def start: Int
  def end: Int
  def pos: Position
  def syntax: String

  def is[U](implicit classifier: Classifier[Token, U]): Boolean
  def isNot[U](implicit classifier: Classifier[Token, U]): Boolean
  def structure: String
}

object Token {
  // Identifiers
  @freeform("identifier") class Ident(value: String) extends Token

  // Alphanumeric keywords
  @fixed("abstract") class KwAbstract extends Token
  @fixed("case") class KwCase extends Token
  @fixed("catch") class KwCatch extends Token
  @fixed("class") class KwClass extends Token
  @fixed("def") class KwDef extends Token
  @fixed("do") class KwDo extends Token
  @fixed("else") class KwElse extends Token
  @fixed("extends") class KwExtends extends Token
  @fixed("false") class KwFalse extends Token
  @fixed("final") class KwFinal extends Token
  @fixed("finally") class KwFinally extends Token
  @fixed("for") class KwFor extends Token
  @fixed("forSome") class KwForsome extends Token
  @fixed("if") class KwIf extends Token
  @fixed("implicit") class KwImplicit extends Token
  @fixed("import") class KwImport extends Token
  @fixed("lazy") class KwLazy extends Token
  @fixed("match") class KwMatch extends Token
  @fixed("macro") class KwMacro extends Token
  @fixed("new") class KwNew extends Token
  @fixed("null") class KwNull extends Token
  @fixed("object") class KwObject extends Token
  @fixed("override") class KwOverride extends Token
  @fixed("package") class KwPackage extends Token
  @fixed("private") class KwPrivate extends Token
  @fixed("protected") class KwProtected extends Token
  @fixed("return") class KwReturn extends Token
  @fixed("sealed") class KwSealed extends Token
  @fixed("super") class KwSuper extends Token
  @fixed("this") class KwThis extends Token
  @fixed("throw") class KwThrow extends Token
  @fixed("trait") class KwTrait extends Token
  @fixed("true") class KwTrue extends Token
  @fixed("try") class KwTry extends Token
  @fixed("type") class KwType extends Token
  @fixed("val") class KwVal extends Token
  @fixed("var") class KwVar extends Token
  @fixed("while") class KwWhile extends Token
  @fixed("with") class KwWith extends Token
  @fixed("yield") class KwYield extends Token

  // Symbolic keywords
  @fixed("#") class Hash extends Token
  @fixed(":") class Colon extends Token
  @fixed("<%") class Viewbound extends Token
  @freeform("<-") class LeftArrow extends Token
  @fixed("<:") class Subtype extends Token
  @fixed("=") class Equals extends Token
  @freeform("=>") class RightArrow extends Token
  @fixed(">:") class Supertype extends Token
  @fixed("@") class At extends Token
  @fixed("_") class Underscore extends Token

  // Delimiters
  @fixed("(") class LeftParen extends Token
  @fixed(")") class RightParen extends Token
  @fixed(",") class Comma extends Token
  @fixed(".") class Dot extends Token
  @fixed(";") class Semicolon extends Token
  @fixed("[") class LeftBracket extends Token
  @fixed("]") class RightBracket extends Token
  @fixed("{") class LeftBrace extends Token
  @fixed("}") class RightBrace extends Token

  // Literals (include some keywords from above, constants, interpolations and xml)
  object Constant {
    @freeform("integer constant") class Int(value: scala.BigInt) extends Token
    @freeform("long constant") class Long(value: scala.BigInt) extends Token
    @freeform("float constant") class Float(value: scala.BigDecimal) extends Token
    @freeform("double constant") class Double(value: scala.BigDecimal) extends Token
    @freeform("character constant") class Char(value: scala.Char) extends Token
    @freeform("symbol constant") class Symbol(value: scala.Symbol) extends Token
    @freeform("string constant") class String(value: Predef.String) extends Token
  }
  object Interpolation {
    @freeform("interpolation id") class Id extends Token
    @freeform("interpolation start") class Start extends Token
    @freeform("interpolation part") class Part(value: String) extends Token
    @freeform("splice start") class SpliceStart extends Token
    @freeform("splice end") class SpliceEnd extends Token
    @freeform("interpolation end") class End extends Token
  }
  object Xml {
    @freeform("xml start") class Start extends Token
    @freeform("xml part") class Part(value: String) extends Token
    @freeform("xml splice start") class SpliceStart extends Token
    @freeform("xml splice end") class SpliceEnd extends Token
    @freeform("xml end") class End extends Token
  }

  // Trivia
  @fixed(" ") class Space extends Token
  @fixed("\t") class Tab extends Token
  @fixed("\r") class CR extends Token
  @fixed("\n") class LF extends Token
  @fixed("\f") class FF extends Token
  @freeform("comment") class Comment extends Token
  @freeform("beginning of file") class BOF extends Token { def start = 0; def end = 0 }
  @freeform("end of file") class EOF extends Token { def start = content.chars.length; def end = content.chars.length }

  // TODO: Rewrite the parser so that it doesn't need LFLF anymore.
  // NOTE: in order to maintain conceptual compatibility with scala.reflect's implementation,
  // Ellipsis.rank = 1 means .., Ellipsis.rank = 2 means ..., etc
  // TODO: after we bootstrap, Unquote.tree will become scala.meta.Tree
  // however, for now, we will keep it at Any in order to also support scala.reflect trees
  @freeform("\n\n") private[meta] class LFLF extends Token
  @freeform("ellipsis") private[meta] class Ellipsis(rank: Int) extends Token
  @freeform("unquote") private[meta] class Unquote(tree: Any) extends Token

  implicit def classifiable[T <: Token]: Classifiable[T] = null
  implicit def showStructure[T <: Token]: Structure[T] = TokenStructure.apply[T]
  implicit def showSyntax[T <: Token]: Syntax[T] = TokenSyntax.apply[T]
}

// NOTE: We have this unrelated code here, because of how materializeAdt works.
// TODO: Revisit this since we now have split everything into separate projects.
//
// Due to an unfortunate limitation of knownDirectSubclasses,
// all macro applications that depend on that API
// must come after the classes that they call the API on.
//
// That's why if we put TokenLiftables elsewhere, we might run into troubles
// depending on how the file and its enclosing directories are called.
// To combat that, we have TokenLiftables right here, guaranteeing that there won't be problems
// if someone wants to refactor/rename something later.
private[meta] trait TokenLiftables extends tokens.Liftables {
  val c: scala.reflect.macros.blackbox.Context
  override lazy val u: c.universe.type = c.universe

  import c.universe._
  private val XtensionQuasiquoteTerm = "shadow scala.meta quasiquotes"

  implicit lazy val liftContent: Liftable[Content] = Liftable[Content] { content =>
    q"_root_.scala.meta.inputs.Input.String(${new String(content.chars)})"
  }

  implicit lazy val liftDialect: Liftable[Dialect] = Liftable[Dialect] { dialect =>
    q"_root_.scala.meta.Dialect.forName(${dialect.name})"
  }

  implicit lazy val liftBigInt: Liftable[BigInt] = Liftable[BigInt] { v =>
    q"_root_.scala.math.BigInt(${v.bigInteger.toString})"
  }

  implicit lazy val liftBigDecimal: Liftable[BigDecimal] = Liftable[BigDecimal] { v =>
    q"_root_.scala.math.BigDecimal(${v.bigDecimal.toString})"
  }

  lazy implicit val liftUnquote: Liftable[Token.Unquote] = Liftable[Token.Unquote] { u =>
    c.abort(c.macroApplication.pos, "fatal error: this shouldn't have happened")
  }

  // TODO: this can't be `implicit val`, because then the materialization macro will crash in GenICode
  implicit def liftToken: Liftable[Token] = materializeToken[Token]

  implicit lazy val liftTokens: Liftable[Tokens] = Liftable[Tokens] { tokens =>
    def prepend(tokens: Tokens, t: Tree): Tree =
      (tokens foldRight t) { case (token, acc) => q"$token +: $acc" }

    def append(t: Tree, tokens: Tokens): Tree =
      // We call insert tokens again because there may be things that need to be spliced in it
      q"$t ++ ${insertTokens(tokens)}"

    def insertTokens(tokens: Tokens): Tree = {
      val (pre, middle) = tokens span (!_.is[Token.Unquote])
      middle match {
        case Tokens() =>
          prepend(pre, q"_root_.scala.meta.tokens.Tokens()")
        case Token.Unquote(tree: Tree) +: rest =>
          // If we are splicing only a single token we need to wrap it in a Tokens
          // to be able to append and prepend other tokens to it easily.
          val quoted = if (tree.tpe <:< typeOf[Token]) q"_root_.scala.meta.tokens.Tokens($tree)" else tree
          append(prepend(pre, quoted), Tokens(rest: _*))
      }
    }

    insertTokens(tokens)
  }
}
