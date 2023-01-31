grammar FilterArg;

CONJUNCTION: 'and' | 'AND';
DISJUNCTION: 'or' | 'OR';

GE: '>';
GEQ: '>=';
LE: '<';
LEQ: '<=';
EQ: '=';
NEQ: '!=';
LIKE: 'like';
IN: 'in';

fragment FIRST_CHAR_IN_PROPERTY: [a-zA-Z];
fragment CHAR_IN_PROPERTY: [a-zA-Z0-9];
fragment PROPERTY_ACCESS: '.' FIRST_CHAR_IN_PROPERTY CHAR_IN_PROPERTY*;
PROPERTY: FIRST_CHAR_IN_PROPERTY CHAR_IN_PROPERTY* PROPERTY_ACCESS*;

fragment CHAR: ~[()," ];
fragment CHAR_IN_QUOTE: CHAR | '\\"';
STRING: CHAR+;
QUOTED_STRING: '"' CHAR_IN_QUOTE* '"';

WS : ' '+ -> skip;

// we also allow ',' for delimiting disjunctions
disjunction: DISJUNCTION | ',';

operator: GE | GEQ | LE | LEQ | EQ | NEQ | LIKE;
collectionOperator: IN;

// we include tokens that can be treated as strings
scalar: STRING | QUOTED_STRING | PROPERTY | CONJUNCTION | DISJUNCTION | operator | collectionOperator;
collection: '(' scalar (',' scalar)* ')';
subClause: PROPERTY operator scalar | PROPERTY collectionOperator collection;
clause: subClause (disjunction subClause)*;
filter: clause (CONJUNCTION clause)* EOF | EOF;