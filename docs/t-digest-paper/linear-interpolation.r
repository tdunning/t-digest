### Illustrates the piece-wise linear approximation of the cumulative distribution using constant size bins
fade = rgb(0,0,0,alpha=0.5)
dot.size = 0.7
n = 10000
set.seed(5)

pdf("linear-interpolation.pdf", width=6, height=2.7, pointsize=10, family='serif')
layout(matrix(c(1,2),byrow=T, ncol=2), widths=c(1.1,1))
u = sort(runif(n))
x = log(1-u)
x = sort(x)
F = ((0:(n-1))+0.5)/n
par(mar=c(2.5,3,0.5,1))
plot(x, F, cex=dot.size, pch=21, bg=fade, col=NA, type='b', xlim=c(-9,-4.5), ylim=c(0,0.01), xaxt='n', ylab=NA, mgp=c(1,0.5,0), xlab=NA)

axis(side=1, at=-10:-1, labels=NA)
title(xlab=expression(italic(x)), line=0.8, cex.lab=1.5)
title(ylab=expression(italic(q)), line=1.5, cex.lab=1.5)

left.end = x[1] - (x[2]-x[1])

lines(c(left.end, x[100]), c(0, 0.01), lwd=2)
lines(c(left.end, left.end), c(-0.0005, 0.0005), lt=1, col='black', lwd=0.5)
lines(c(x[100], x[100]), c(0.0085, 0.015), lt=1, col='black', lwd=0.5)
text(-7, 0.006, "100")

###text(-5, 0.4, adj=0, "Constant size bins result in large")
###text(-5, 0.35, adj=0, "errors at extreme quantiles")

par(mar=c(2.5,1.5,0.5,1))

plot(x, F, cex=dot.size, pch=21, bg=fade, col=NA, type='b', xlim=c(-9,-4.5), ylim=c(0,0.01), yaxt='n', xaxt='n')
axis(side=1, at=-10:-1, labels=NA)
axis(side=2, at=(0:6)/10, labels=NA)
title(xlab=expression(italic(x)), line=0.8, cex.lab=1.5)
title(ylab=expression(italic(q)), line=2, cex.lab=1.5)

q.to.k = function(q) {
    (asin(2*q-1)/pi + 1/2)
}

k.to.q = function(k,compression) {
    sin(k/compression*pi - pi/2)/2 + 0.5
}

weights = c(2, 8, 19, 35, 56, 81, 111)
q.bin = cumsum(c(0, weights)/n)

i.bin = c(1, cumsum(weights)+1)
i.right = i.bin-1
i.right = i.right[i.right > 0]
m = length(i.right)
i.bin = i.bin[1:m]

x.bin = c(left.end, (x[i.right[1:(m-1)]] + x[i.bin[2:m]])/2)
F.bin = (i.bin-1) / n
lines(x.bin, F.bin, lwd=2)
dy = 0.0005
for (i in 1:m) {
    x.text = (x[i.bin[i]] + x[i.right[i]])/2
    y.text = (F.bin[i] + F.bin[i+1])/2
    x.offset = 0.3 * y.text
    y.offset = dy * (1 + 500*y.text)
    x.pos = x.text - x.offset
    y.pos = y.text + y.offset
    lines(c(x.bin[i],x.bin[i]), c(F.bin[i]-dy+0.000, F.bin[i]+dy+y.offset*0.6-0.0005), lt=1, lwd=0.5, col='black')
    text(x.text - x.offset, y.text + y.offset, i.right[i]-i.bin[i]+1)
}
###text(-5, 0.35, adj=0, "Variable size bins keep errors")
###text(-5, 0.3, adj=0, "small at extreme quantiles")

dev.off()
