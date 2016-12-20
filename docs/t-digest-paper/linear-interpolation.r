### Illustrates the piece-wise linear approximation of the cumulative distribution using constant size bins

fade = rgb(0,0,0,alpha=0.5)
dot.size = 0.7
n = 10000
set.seed(5)

pdf("linear-interpolation.pdf", width=6, height=2.7, pointsize=10)
layout(matrix(c(1,2),byrow=T, ncol=2), widths=c(1.1,1))

u = sort(runif(n))
x = log(1-u)
x = sort(x)
F = ((0:(n-1))+0.5)/n

par(mar=c(1.5,1.,1,1))
plot(x, F, cex=0.5, pch=21, bg='gray', col='gray', type='b', xlim=c(-5,-0.75), ylim=c(0,0.6), yaxt='n', xaxt='n')
axis(side=1, at=-5:-1, labels=NA)
axis(side=2, at=(0:6)/10, labels=NA)
i.bin = 10*(0:(n/10))+1
i.right = i.bin-1
i.right = i.right[i.right > 0]
m = length(i.right)
i.bin = i.bin[1:m]

x.bin = rep(0, m+1)
x.bin[1] = x[1] - (x[2]-x[1])
i = 2:m
x.bin[i] = (x[i.bin[i]-1] + x[i.bin[i]])/2
F.bin = (0:m)/10
lines(x.bin,F.bin)
dy = 0.04
for (i in 1:(m+1)) {
    lines(c(x.bin[i],x.bin[i]), c(F.bin[i]-dy, F.bin[i]+dy), lt=1, col='grey')
    x.text = (x[i.bin[i]] + x[i.right[i]])/2
    y.text = (F.bin[i] + F.bin[i+1])/2
    x.offset = 0.7 * y.text
    y.offset = dy * (1 + 3*y.text)
    print(c(i,x.text, y.text, y.offset, x.bin[i], x[i.right[i]]))
    text(x.text - x.offset, y.text + y.offset, i.right[i]-i.bin[i]+1)
}
###text(-5, 0.4, adj=0, "Constant size bins result in large")
###text(-5, 0.35, adj=0, "errors at extreme quantiles")

par(mar=c(1.5,0,1,1))

plot(x, F, cex=0.5, pch=21, bg='gray', col='gray', type='b', xlim=c(-5,-0.75), ylim=c(0,0.6), yaxt='n', xaxt='n')
axis(side=1, at=-5:-1, labels=NA)
axis(side=2, at=(0:6)/10, labels=NA)
q.to.k = function(q,compression) {
    compression * (asin(2*q-1)/pi + 1/2)
}

k.to.q = function(k,compression) {
    sin(k/compression*pi - pi/2)/2 + 0.5
}

k.bin = 0:10
q.bin = k.to.q(k.bin, compression=10)

i.bin = floor(n * q.bin) + 1
i.right = i.bin-1
i.right = i.right[i.right > 0]
m = length(i.right)
i.bin = i.bin[1:m]

x.bin = c(x[1] - (x[2]-x[1]), (x[i.right[1:(m-1)]] + x[i.bin[2:m]])/2)
F.bin = (i.bin-1) / 100
lines(x.bin,F.bin)
dy = 0.04
for (i in 1:m) {
    lines(c(x.bin[i],x.bin[i]), c(F.bin[i]-dy, F.bin[i]+dy), lt=1, col='grey')
    x.text = (x[i.bin[i]] + x[i.right[i]])/2
    y.text = (F.bin[i] + F.bin[i+1])/2
    x.offset = 0.7 * y.text
    y.offset = dy * (1 + 3*y.text)
    print(c(i,x.text, y.text, y.offset, x.bin[i], x[i.right[i]]))
    text(x.text - x.offset, y.text + y.offset, i.right[i]-i.bin[i]+1)
}
###text(-5, 0.35, adj=0, "Variable size bins keep errors")
###text(-5, 0.3, adj=0, "small at extreme quantiles")

dev.off()
