plot(c(), c(), ylim=c(0,40), xlim=c(0,1), yaxt='n')
axis(side=2, at=(0:4)*10, labels=(0:4)*50)
r = matrix(0, nrow = 40, ncol=11)
for (y in 2:40) {
    n = 5*y
    m = 50 * (10000/n)
    sum = rep(0, len=11)
    for (i in 1:m) {
        sum = sum + quantile(runif(n=y), seq(0,1,0.1))
    }
    r[y,] = sum/m
    print(y)
}
for (x in 1:11) {
    lines(r[2:40,x], 2:40, type='b')
}


plot(c(), c(), ylim=c(0,40), xlim=c(1e-6,0.03), yaxt='n', log='x')
axis(side=2, at=(0:4)*10, labels=(0:4)*1000)
for (i in 1:40) {
    n = i * 1000
    m = 1e6/n
    r = rep(0,4)
    for (j in 1:m) {
        r = r + quantile(runif(n), c(0, 0.001, 0.01, 0.02))
    }
    r = r/m
    points(r, rep(i, 4))
}
