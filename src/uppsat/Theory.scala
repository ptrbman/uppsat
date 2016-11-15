package uppsat

trait Theory {
    // Sort
    // Symbol, consts, funs, variables
  
    val name : String
    val sorts : Seq[Sort]
    val symbols : Seq[FunctionSymbol]
    
    def parseLiteral(lit : String) : Node
    def isDefinedLiteral(symbol : ConcreteFunctionSymbol) : Boolean
    def SMTHeader : String
    def toSMTLib(symbol : ConcreteFunctionSymbol) : String
    def toSMTLib(sort : ConcreteSort) : String
    def declarationToSMTLib(symbol : ConcreteFunctionSymbol) : String
}