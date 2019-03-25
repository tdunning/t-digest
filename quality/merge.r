require(dplyr)
data = read.csv("merge.csv")

plotMerge = function(n, yaxt = 's') {
  if (yaxt == 'n') {
    ylab = NA
    old = par(mar=c(3.5, 0.5, 2, 0.2))
  } else {
    ylab = "Absolute error (ppm)"
    old = par(mar=c(3.5, 4.0, 2, 0.2))
  }
  par(las=1)
  par(lwd=0.5)
  par(cex.lab=1.3)
  par(cex.axis=0.9)
  par(mgp=c(2.4, 0.5, 0))
  par(tcl=-0.3)

  our.data = data %>% filter(type == "quantile", parts == n)
  boxplot(e1*1e6 ~ q, at=(1:6)-0.23, xaxt='n', boxwex=0.19, our.data,
            ylim=c(-3000, 3000), cex=0.5, yaxt = yaxt,
          col=rgb(0.95, 0.95, 0.95), 
          xlab=NA, ylab=NA)
  title(xlab=expression('Quantile '(italic(q))), mgp=c(2.2, 0.5, 0))
  title(ylab=ylab, mgp=c(2.8, 0.0, 0))
  boxplot(e3*1e6 ~ q, at=1:6, xaxt='n', boxwex=0.19, add=T, our.data,
          col=rgb(0.7, 0.7, 0.7), cex=0.5, yaxt = yaxt)
  boxplot(e2*1e6 ~ q, at=1:6+0.23, xaxt='n', boxwex=0.19, add=T, our.data,
          col=rgb(0.4, 0.4, 0.4), cex=0.5, yaxt = yaxt)
  axis(side=1, at=1:6,
       labels=c(expression(10^-3), expression(10^-2), 0.1, 0.2, 0.3, 0.5),
       )
  legend(0.13, -1300,
         expression("Direct "(delta==100),
                    "Stratified merge "(delta==200,100),
                    "Flat merge "(delta==100,100)), 
         fill = c(rgb(0.95, 0.95, 0.95), rgb(0.7, 0.7, 0.7), rgb(0.4, 0.4, 0.4)),
         cex=0.75)
  abline(h=0, col=rgb(0.4, 0.4, 0.4))
  title(paste(n, " parts"), cex.main=1.3)
  box()
  par(old)
}

#setEPS()
pdf("merge.pdf", width=6, height=2.4, pointsize=9, family='serif')
layout(matrix(c(1,2,3), 1, 3, byrow=T), widths=c(1.285,1,1))
par(cex=1)

plotMerge(5, 's')
plotMerge(20, 'n')
plotMerge(100, 'n')

dev.off()
