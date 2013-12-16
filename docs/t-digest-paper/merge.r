data = read.delim("merge.tsv")

plotMerge = function(n, yaxt = 's') {
  if (yaxt == 'n') {
    ylab = NA
    old = par(mar=c(4.5,1,3,0.5))
  } else {
    ylab = "Error (ppm)"
    old = par(mar=c(4.5,5,3,0.5))
  }
  boxplot(e1*1e6 ~ q, at=1:6-0.17, xaxt='n', boxwex=0.25, data[data$type=="quantile" & data$parts == n,],
          ylim=c(-10000, 10000), cex=0.5, yaxt = yaxt,
          col=rgb(0.95, 0.95, 0.95), xlab='Cumulative Distribution (q)', ylab=ylab)
  boxplot(e2*1e6 ~ q, at=1:6+0.17, xaxt='n', boxwex=0.25, add=T, data[data$type=="quantile" & data$parts == n,],
          col=rgb(0.4, 0.4, 0.4), cex=0.5, yaxt = yaxt)
  axis(side=1, at=1:6, labels=c(0.001, 0.01, 0.1, 0.2, 0.3, 0.5))
  legend(0.5, 10000, c("Direct", "Merged"), fill = c(rgb(0.95, 0.95, 0.95), rgb(0.4, 0.4, 0.4)))
  text(1, 6000, expression(1/delta == 50))
  title(paste(n, " parts"))
  box(lwd=3)
  par(old)
}

png("merge.png", width=1800, height=600, pointsize=28)
layout(matrix(c(1,2,3), 1, 3, byrow=T), widths=c(1.15,1,1))

plotMerge(5, 's')
plotMerge(20, 'n')
plotMerge(100, 'n')

dev.off()
