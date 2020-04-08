move(rock).
move(paper).
move(scissor).

greater(paper, rock).
greater(rock, scissor).
greater(scissor, paper).

isGreater(X, Y) :- move(X), move(Y), greater(X, Y).

win(X, Y, X):- isGreater(X, Y).
win(X, Y, Y):- isGreater(Y, X).
win(X, X, draw):- move(X).
