plotCellHist = function(deviation, plot, title) {
  hist(deviation$deviation, breaks=seq(-5,5,by=0.1), main=title, xlab=NA, ylab=NA, lwd=6,
       col='grey', xlim=c(-1.0, 1.0), border=NA)
  abline(v=-0.5, lty=2, lwd=6)
  abline(v=0.5, lty=2, lwd=6)
}

deviation = read.delim("deviation.csv")
png("deviation.png", width=2700, height=700, pointsize=64)
layout(matrix(c(1,2,3), 1, 3, byrow=T), heights=c(700,200))
#layout.show(4)
old = par(mar=c(2.5,2,3,0.5))
plotCellHist(deviation[deviation$tag == "uniform" & deviation$Q > 0.3 & deviation$Q < 0.7,], "uniform-deviation.png", "Uniform q=0.3 ... 0.7")
plotCellHist(deviation[deviation$tag == "gamma" & deviation$Q > 0.3 & deviation$Q < 0.7,], "gamma-deviation.png", "Gamma(0.1, 0.1) q=0.3 ... 0.7")

plotCellHist(deviation[deviation$tag == "gamma" & deviation$mean > 0.01 & deviation$mean < 0.015,], "gamma-low.png", "Gamma, q=0.01")
par(old)
dev.off()


