grammar SimpleC;

program:      function+ ;

function:     ID '(' formalParams? ')' '{' decl* stmt*'}' ;

formalParams:   ID (',' ID)* ;

decl:         type ID ';' ;

type:          'int' | 'bool' ;

stmt:         'input' ID ';' # Input
            | 'output' expr ';' # Output
            | ID '=' expr ';' # Assignment
            | 'while' '(' expr ')' stmt # While
            | 'if' '(' expr ')' stmt 'else' stmt #IfThenElse
            | 'return' expr ';' # Return
            | 'skip' ';' # Skip
            | '{' stmt* '}' # Compound
            ;
expr:         ID '(' actualParams? ')' # Call
            | '-' expr # Negate
            | '!' expr # Not
            | expr op=('*'|'/') expr # MultiDiv
            | expr op=('+'|'-') expr # AddSub
            | expr op=('<'|'>'|'<='|'>=') expr # Relational
            | expr op=('=='|'!=') expr # EqNeq
            | expr op=('&&'|'||') expr # AndOr
            | ID # Var
            | NUM # Num
            | '(' expr ')' # Parens
            ;

actualParams:   expr (',' expr)* ;

// lexical spec
INT: 'int' ;
BOOL: 'bool' ;
ID: [a-zA-Z] ([a-zA-Z] | [0-9])* ;
NUM: [0-9]+ ;
MULT: '*' ;
DIV: '/' ;
PLUS: '+' ;
MINUS: '-' ;
AND: '&&' ;
OR: '||' ;
EQ: '==' ;
NEQ: '!=' ;
LT: '<' ;
GT: '>' ;
LE: '<=' ;
GE: '>=' ;
WS:   [ \t\n\r] + -> skip;
SL_COMMENT:   '//' .*? '\n' -> skip;
