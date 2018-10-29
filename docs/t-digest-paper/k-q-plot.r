n = 10
fade = 'darkgray'
pdf("k-q-plot.pdf", width=2.5, height=2.2, pointsize=8)

par(mar=c(3.,3,1,1))

q.to.k = function(q,compression) {
    compression * (asin(2*q-1)/pi/2)
}

k.to.q = function(k,compression) {
    sin(k/compression*pi - pi/2)/2 + 0.5
}

q = seq(0,1,by=0.001)

plot(q, q.to.k(q, compression=n), type='l', lwd=2, xlab=NA, ylab=NA, xaxt='n', yaxt='n')
axis(side=1, at=(0:5)/5, mgp=c(1,0.5,0))
title(xlab='q', line=1.3, cex.lab=1.5)
axis(side=2, at=seq(-5,5,by=1), mgp=c(1,0.6,0))
title(ylab='k', line=1.5, cex.lab=1.5)

for (i in 0:n) {
    abline(h=i-5, col=fade)
    abline(v=k.to.q(i, compression=n), col=fade)
}
lines(q, q.to.k(q, compression=n), type='l', lwd=2)
dev.off()
