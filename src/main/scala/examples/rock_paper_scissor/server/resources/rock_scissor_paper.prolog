move(rock).
move(paper).
move(scissor).
move(lizard).
move(spock).

greater(paper, rock).
greater(rock, scissor).
greater(scissor, paper).
greater(rock, lizard).
greater(lizard, spock).
greater(spock, scissor).
greater(lizard, paper).
greater(paper, spock).
greater(spock, rock).
greater(scissor, lizard).


isGreater(X, Y) :- move(X), move(Y), greater(X, Y).

win(X, Y, X):- isGreater(X, Y).
win(X, Y, Y):- isGreater(Y, X).
win(X, X, draw):- move(X).
