fig.no = 1
pdf(sprintf("plot-%03d.pdf", fig.no),
            width=7, height=5, pointsize=10)
fig.no = fig.no + 1

# This figure shows the mapping between q and k and how variable size clusters result.
par(cex.lab=1.5)
par(cex.axis=1.5)
scale = 30
q.marks = (sin(seq(-pi/2+0.01,pi/2-0.01,length.out=16))+1)/2
plot(q.marks, scale*asin(2*q.marks-1)/pi+scale/2, xlim=c(0,1.05), ylim=c(-3,scale), 
    lwd=2, cex=0.7, 
    type='b', ylab='k', xlab='q')
for (i in 1:(length(q.marks))) {
    q = q.marks[i]
    lines(c(q,q), c(-3, scale*asin(2*q-1)/pi + scale/2 -1), lwd=2, col='gray')
}
dev.off()

pdf(sprintf("plot-%03d.pdf", fig.no),
            width=7, height=5, pointsize=10)
fig.no = fig.no + 1

# this shows the old and sqrt limits
par(cex.lab=1.5)
par(cex.axis=1.5)
q = seq(0, 1, by=0.001)
plot(q, 6*q*(1-q), type='l', lwd=2, ylab="Cluster Size")
lines(q, 8/pi*sqrt(q*(1-q)), lwd=2, lty=2)

dev.off()

pdf(sprintf("plot-%03d.pdf", fig.no),
            width=7, height=5, pointsize=10)
fig.no = fig.no + 1

par(cex.lab=1.5)
par(cex.axis=1.5)
plot(q, 100*(asin(2*q-1)/pi+0.5), type='l', lwd=2,
    ylab="k")

dev.off()
