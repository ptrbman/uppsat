package uppsat.solver;

import java.io.ByteArrayInputStream;
import sys.process._
import scala.sys.process.stringToProcess
import uppsat.solver._
import java.io.BufferedReader;
import java.io.InputStreamReader;
import uppsat.Timer
import uppsat.ast.ConcreteFunctionSymbol

class Z3OnlineException(msg : String) extends Exception("Z3 error: " + msg)

// Starts process at 

class Z3OnlineSolver extends SMTSolver {
  var silent = false
  
  def setSilent(b : Boolean) = {
    silent = b
  }
  
  def z3print(str : String) =
    if (!silent)
      println("[Z3] " + str)
  
  // Starting solver...
  val process = Runtime.getRuntime().exec("z3 -in")
  z3print("[Started process: " + process)
  val stdin = process.getOutputStream ()
  val stderr = process.getErrorStream ()
  val stdout = process.getInputStream () 
  val outReader = new BufferedReader(new InputStreamReader (stdout))
  
  def init() = {
    val f = "(reset)\n(check-sat)\n"
    feedInput(f)
    val r = catchOutput(f) 
    r match { 
      case Some("sat") => println("Init caught sat")
      case _ => throw new Exception("Empty check-sat failed to return sat : " + r)
    }
  }
  
  def feedInput(f : String) = {
    z3print("Sending ... \n" + f)
    stdin.write((f+"\n").getBytes());
    stdin.flush();
  }
  
  def catchOutput(formula : String) = {
    
    var result = None : Option[String]    

    val errorPattern = ".*error.*".r
    val satPattern = "sat".r
    val unsatPattern = "unsat".r
    
    z3print("Collecting output ")
    var line = None : Option[String]
    while (result.isEmpty) {
      z3print(".")
      line = Option(outReader.readLine())
      z3print(".")
      println(line)
      line.get match {
        case errorPattern() => 
          println(formula)
          throw new Exception("Z3 error: " + line.get)
        case other => result = Some(other)
      }    
    }
    result
  }
  
  def evaluate(formula : String) : String = {
    evaluate(formula, List()).head
  }
  
  def evalSymbol( symbol : ConcreteFunctionSymbol) = {
    val formula = "(eval " + symbol + ")" 
    feedInput(formula)
    catchOutput(formula)
  }
  
  // Evaluation of a an expression, should always result in a value
  def evaluateExpression(expression : String) = Timer.measure("Z3OnlineSolver.runSolver") {
    init
    z3print("Evaluating: \n" + expression)    
    feedInput(expression)
    catchOutput(expression).get 
  }
    
  def evaluate(formula : String, answers : List[ConcreteFunctionSymbol] = List()) : List[String] = Timer.measure("Z3OnlineSolver.runSolver") {
    init
    z3print("Evaluating: \n" + formula)    
    feedInput(formula)
    catchOutput(formula) match {
      case Some("sat") => answers.map(evalSymbol(_)).collect { case Some(x) => x }
      case Some("unsat") => List()
    }
  }

  
  def reset = {    
    feedInput("(reset)\n")
  }
  
  def parseOutput(output : String, extractSymbols : List[String]) : Option[Map[String, String]] = {
    val lines = output.split("\n")
    val result = lines.head.trim()
    if (result != "sat")
      throw new Exception("Trying to get model from non-sat result (" + result + ")")
    
    val model = (extractSymbols zip lines.tail).toMap
    Some(model)
  }
  
  def getStringModel(formula : String, extractSymbols : List[String]) = {
    val extendedFormula = formula + (extractSymbols.map("(eval " + _ + ")").mkString("\n", "\n", ""))
    val result = evaluate(extendedFormula)
    parseOutput(result, extractSymbols).get    
  }
  
  def checkSat(formula : String) : Boolean = {
    val result = evaluate(formula)  
    val retVal = result.split("\n").head.trim()
    retVal match {
      case "sat" => true
      case "unsat" => false
      case str => throw new Exception("Unexpected sat/unsat result: " + str)
    }
  }

  //Not used by the online solver
  def getAnswer(formula : String) : String = {
    val result = evaluate(formula)  
    val retVal = result.split("\n")
    retVal.head.trim() match {
      case "sat" => retVal(1).trim()
      case str => throw new Exception("Unexpected sat/unsat result: " + str)
    }
  }
  
  
  def stopSolver() = {
    process.destroy()
    process.waitFor()
  }

}
