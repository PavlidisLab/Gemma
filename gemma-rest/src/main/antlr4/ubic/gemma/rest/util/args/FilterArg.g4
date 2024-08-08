grammar FilterArg;

CONJUNCTION: 'and' | 'AND';
DISJUNCTION: 'or' | 'OR';

GE: '>';
GEQ: '>=';
LE: '<';
LEQ: '<=';
EQ: '=';
NEQ: '!=';
LIKE: 'like' | 'LIKE';
NOT_LIKE: 'not like' | 'NOT LIKE';
IN: 'in' | 'IN';
NOT_IN: 'not in' | 'NOT IN';

ANY: 'any' | 'ANY';
NONE: 'none' | 'NONE';
ALL: 'all' | 'ALL';

fragment FIRST_CHAR_IN_PROPERTY: [a-zA-Z];
fragment CHAR_IN_PROPERTY: [a-zA-Z0-9];
fragment PROPERTY_ACCESS: '.' FIRST_CHAR_IN_PROPERTY CHAR_IN_PROPERTY*;
PROPERTY: FIRST_CHAR_IN_PROPERTY CHAR_IN_PROPERTY* PROPERTY_ACCESS*;

fragment CHAR: ~[()," ];
fragment CHAR_IN_QUOTE: CHAR | [(), ] | '\\"';
STRING: CHAR+;
QUOTED_STRING: '"' CHAR_IN_QUOTE* '"';

WS : ' '+ -> skip;

// we also allow ',' for delimiting disjunctions
disjunction: DISJUNCTION | ',';

operator: GE | GEQ | LE | LEQ | EQ | NEQ | LIKE | NOT_LIKE;
collectionOperator: IN | NOT_IN;

quantifier: ALL | ANY | NONE;

// we include tokens that can be treated as strings
scalar: STRING | QUOTED_STRING | PROPERTY | CONJUNCTION | DISJUNCTION | ANY | NONE | ALL | operator | collectionOperator;
collection: '(' scalar (',' scalar)* ')' | '(' ')';
predicate: PROPERTY operator scalar | PROPERTY collectionOperator collection;
subClause: predicate | quantifier '(' predicate ')';
clause: subClause (disjunction subClause)*;
filter: clause (CONJUNCTION clause)* EOF | EOF;