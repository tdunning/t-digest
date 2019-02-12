\NeedsTeXFormat{LaTeX2e}[1995/12/01]
\ProvidesClass{statsoc}
\DeclareOption{draft }{\setlength\overfullrule{5pt}}
\DeclareOption{proofs}{\setlength\overfullrule{5pt}}
\DeclareOption{crc   }{\setlength\overfullrule{0pt}}
\DeclareOption{final }{\setlength\overfullrule{0pt}}
\DeclareOption{leqno}{\input{leqno.clo}}
\DeclareOption{fleqn}{\input{fleqn.clo}}
\DeclareOption{mathptm}{%
  \def\JOURNAL@fonts{\RequirePackage{times,mathptm}}%
  \renewcommand{\ttdefault}{cmtt}}
 \renewcommand{\sfdefault}{phv}
\DeclareOption{mathtime}{%
  \def\JOURNAL@fonts{\RequirePackage{times}\RequirePackage%
{mathtime}}%
 \renewcommand{\sfdefault}{phv}
  \renewcommand{\ttdefault}{cmtt}}
\DeclareOption{cmfonts}{%
 \renewcommand{\sfdefault}{phv}
 \let\JOURNAL@fonts\relax}
\ExecuteOptions{cmfonts,final}
    \setlength\paperheight {58.5pc}
    \setlength\paperwidth  {41pc}
    \@twosidetrue\@mparswitchtrue
\ProcessOptions
\JOURNAL@fonts
\RequirePackage{graphics}
\RequirePackage{latexsym}
\DeclareMathAlphabet\mathbi
  \encodingdefault\rmdefault\bfdefault\itdefault
\DeclareMathAlphabet\mathsc
  \encodingdefault{cmr}\mddefault\scdefault
\renewcommand\normalsize{%
   \@setfontsize\normalsize\@xpt\@xiipt
   \abovedisplayskip 10\p@ \@plus2\p@ \@minus5\p@
   \abovedisplayshortskip \z@ \@plus3\p@
   \belowdisplayshortskip 6\p@ \@plus3\p@ \@minus3\p@
   \belowdisplayskip \abovedisplayskip
   \let\@listi\@listI}
\normalsize
\newcommand\small{%
   \@setfontsize\small\@ixpt{11}%
   \abovedisplayskip 8.5\p@ \@plus3\p@ \@minus4\p@
   \abovedisplayshortskip \z@ \@plus2\p@
   \belowdisplayshortskip 4\p@ \@plus2\p@ \@minus2\p@
   \def\@listi{\leftmargin\leftmargini
               \topsep 4\p@ \@plus2\p@ \@minus2\p@
               \parsep 2\p@ \@plus\p@ \@minus\p@
               \itemsep \parsep}%
   \belowdisplayskip \abovedisplayskip
}
\newcommand\footnotesize{%
   \@setfontsize\footnotesize\@viiipt{9.5}%
   \abovedisplayskip 6\p@ \@plus2\p@ \@minus4\p@
   \abovedisplayshortskip \z@ \@plus\p@
   \belowdisplayshortskip 3\p@ \@plus\p@ \@minus2\p@
   \def\@listi{\leftmargin\leftmargini
               \topsep 3\p@ \@plus\p@ \@minus\p@
               \parsep 2\p@ \@plus\p@ \@minus\p@
               \itemsep \parsep}%
   \belowdisplayskip \abovedisplayskip
}
\newcommand\scriptsize{\@setfontsize\scriptsize\@viipt\@viiipt}
\newcommand\tiny{\@setfontsize\tiny\@vpt\@vipt}
\newcommand\large{\@setfontsize\large\@xiipt{14}}
\newcommand\Large{\@setfontsize\Large\@xivpt{18}}
\newcommand\LARGE{\@setfontsize\LARGE\@xviipt{22}}
\newcommand\huge{\@setfontsize\huge\@xxpt{25}}
\newcommand\Huge{\@setfontsize\Huge\@xxvpt{30}}
\setlength\lineskip{1\p@}
\setlength\normallineskip{1\p@}
\renewcommand\baselinestretch{}
\setlength\parskip{0\p@ \@plus \p@}
\setlength\parindent{15\p@}
\setlength\smallskipamount{3\p@ \@plus 1\p@ \@minus 1\p@}
\setlength\medskipamount{6\p@ \@plus 2\p@ \@minus 2\p@}
\setlength\bigskipamount{12\p@ \@plus 4\p@ \@minus 4\p@}
\@lowpenalty   51
\@medpenalty  151
\@highpenalty 301
\setlength\headheight{9\p@}
\setlength\headsep   {9\p@}
\setlength\topskip   {10\p@}
\setlength\footskip{30\p@}
\setlength\maxdepth{0.5\topskip}
%%%%%%Updation
\setlength\textwidth{33pc}
%%\setlength\textwidth{40pc}
%%%%%%%
  \@settopoint\textwidth
%%%%%%Updation
\setlength\textheight{48\baselineskip}
%%\setlength\textheight{58\baselineskip}
%%%%%%
  \addtolength\textheight{\topskip}
  \@settopoint\textheight
\setlength\marginparsep{11\p@}
\setlength\marginparpush{5\p@}
%%%%%%Updation
\setlength\oddsidemargin   {4pc}
%%\setlength\oddsidemargin   {0pc}
%%%%%%
  \@settopoint\oddsidemargin
%%%%%%Updation
\setlength\evensidemargin  {3.5pc}
%%\setlength\evensidemargin   {0pc}
%%%%%%
  \@settopoint\evensidemargin
\setlength\marginparwidth {107\p@}
  \@settopoint\marginparwidth
%%%%%%Updation
\setlength\topmargin{3pc}
%%\setlength\topmargin{0pc}
%%%%%%
  \@settopoint\topmargin
\setlength\footnotesep{6.65\p@}
\setlength{\skip\footins}{9\p@ \@plus 4\p@ \@minus 2\p@}
\setcounter{topnumber}{2}
\renewcommand\topfraction{0.7}
\setcounter{bottomnumber}{1}
\renewcommand\bottomfraction{0.3}
\setcounter{totalnumber}{3}
\renewcommand\textfraction{0.2}
\renewcommand\floatpagefraction{0.5}
\setlength\floatsep    {12\p@ \@plus 2\p@ \@minus 2\p@}
\setlength\textfloatsep{20\p@ \@plus 2\p@ \@minus 4\p@}
\setlength\intextsep   {12\p@ \@plus 2\p@ \@minus 2\p@}
\setlength\@fptop{0\p@ \@plus 1fil}
\setlength\@fpsep{8\p@ \@plus 2fil}
\setlength\@fpbot{0\p@ \@plus 1fil}
\def\artid#1{\gdef\@artid{#1}}
\def\volume#1{\setcounter{@volumenumber}{#1}}
  \newcounter{@volumenumber}
  \setcounter{@volumenumber}{1}
\def\issue#1{\setcounter{@issuenumber}{#1}}
  \newcounter{@issuenumber}
  \setcounter{@issuenumber}{1}
\def\four@digits#1{\ifnum#1<1000 0\fi\ifnum#1<100 0\fi\ifnum#1<10
0\fi\number#1}
\def\startpage{\@ifundefined{r@firstpage}{????}%
   {\expandafter\four@digits{\expandafter\@secondoftwo\r@firstpage}}}
\def\pages{\@ifundefined{r@lastpage}{??}%
  {\addtocounter{@pages}{ \expandafter\@secondoftwo\r@lastpage}%
   \addtocounter{@pages}{-\expandafter\@secondoftwo\r@firstpage}%
   \two@digits{\value{@pages}}}}
  \newcounter{@pages}
  \setcounter{@pages}{1}
\def\journalname#1{\gdef\@journalname{#1}}
\def\journalnumber#1{\gdef\@journalnumber{#1}}
\def\copyrightname#1{\gdef\@copyrightname{#1}}
\def\price#1{\gdef\@price{#1}}
\artid{??????}
\volume{1}
\issue{1}
\journalname{Appl.\ Statist.}
\journalnumber{0035-9254}
\copyrightname{Royal Statistical Society}
\price{\$141.00}
\def\label@firstpage{%
   \immediate\write\@auxout{\string
   \newlabel{firstpage}{{}{\thepage}}}%
}
\def\label@lastpage{%
  \advance\c@page\m@ne
   \immediate\write\@auxout{\string
   \newlabel{lastpage}{{}{\thepage}}}%
   \advance\c@page\@ne}
\def\ps@title{%
      \let\@oddfoot\@empty\let\@evenfoot\@empty
      \let\@evenhead\@empty\let\@oddhead\@empty}
  \def\ps@statsoc{%
      \let\@oddfoot\@empty\let\@evenfoot\@empty

\def\@evenhead{{\sffamily\small\thepage\qquad\slshape\@shortauthor\hfil}}%

\def\@oddhead{\hfil{\sffamily\small{\slshape\@shorttitle}\qquad\thepage}}%
      \let\@mkboth\markboth
    \def\sectionmark##1{%
      \markboth {\MakeUppercase{%
        \ifnum \c@secnumdepth >\z@
          \thesection\quad
        \fi
        ##1}}{}}%
    \def\subsectionmark##1{%
      \markright {%
        \ifnum \c@secnumdepth >\@ne
          \thesubsection\quad
        \fi
        ##1}}}
  \def\ps@empty{%
      \let\@oddfoot\@empty\let\@evenfoot\@empty
      \let\@evenhead\@empty\let\@oddhead\@empty
  }
\def\affiliation#1{#1}
\def\@email{}
%%\def\email#1{\gdef\@email{{\sffamily E-mail: #1}}}
\def\email#1{\g@addto@macro\@correspondence{{{\sffamily E-mail: #1}}}}%
\let\@coaddress\@empty
\newcommand{\address}[1]{\g@addto@macro\@correspondence{\item[]{\small\itshape #1}\vskip 4\p@}}\let\@accepted\@empty
\def\coaddress#1{\g@addto@macro\@correspondence{\thanks{\textit{Address for correspondence:} #1\\\@email}}}
\def\accepted#1{\gdef\@accepted{#1}}
\let\@received\@empty
\def\received#1{\gdef\@received{#1}}
\let\@revised\@empty
\def\revised#1{\gdef\@revised{#1}}
\def\title{\@dblarg\statsoc@title}
\def\statsoc@title[#1]#2{%
  \def\@title{#2}%
  \def\@shorttitle{#1}}
\def\author{\@dblarg\statsoc@author}
\def\statsoc@author[#1]#2{%
  \g@addto@macro\@correspondence{\item[]{\normalsize #2}}%
  \def\@shortauthor{#1}}
 \let\@correspondence\@empty
  \newcommand\maketitle{%
  \let\footnotesize\small
  \let\footnoterule\relax
  {\let \footnote \thanks
  \global\@topnum\z@
  \null
  {\list{}{\leftmargin=2pc\parsep\z@\itemsep 3\p@}
    \item[]\relax
    {\sffamily\bfseries\Large \@title \par}%
    \vskip 2pc%
    {\sffamily
        {\@correspondence}}%
    {\sffamily\footnotesize
    \ifx\@received\@empty
        \ifx\@revised\@empty
            \ifx\@accepted\@empty
            \else
                \item[] [Final revision \@accepted]%
            \fi
        \else
            \item[] [Revised \@revised%
            \ifx\@accepted\@empty
                  ]
            \else
                . Final revision \@accepted]%
            \fi
         \fi
    \else
      \item[] [Received \@received%
        \ifx\@revised\@empty
            \ifx\@accepted\@empty
                 ]
            \else
                 . Final revision \@accepted]%
            \fi
        \else
                . Revised \@revised%
            \ifx\@accepted\@empty
                 ]
            \else
                . Final revision \@accepted]%
            \fi
         \fi
    \fi}%
    \endlist}
    \thispagestyle{title}\label@firstpage\@thanks}
  \global\let\thanks\relax
  \global\let\maketitle\relax
  \global\let\@thanks\@empty
  \global\let\@author\@empty
  \global\let\@date\@empty
  \global\let\@title\@empty
  \global\let\@coaddress\@empty
  \global\let\title\relax
  \global\let\author\relax
  \global\let\date\relax
  \global\let\and\relax
}
\AtBeginDocument{\maketitle}
\AtEndDocument{\par%
  \clearpage\label@lastpage}
\setcounter{secnumdepth}{3}
\newcounter {section}
\newcounter {subsection}[section]
\newcounter {subsubsection}[subsection]
\newcounter {paragraph}[subsubsection]
\renewcommand\thesection      {\@arabic\c@section}
\renewcommand\thesubsection   {\thesection.\@arabic\c@subsection}
\renewcommand\thesubsubsection{\thesubsection .\@arabic\c@subsubsection}
\renewcommand\theparagraph    {\thesubsubsection.\@arabic\c@paragraph}
\newcommand\section{\@startsection {section}{1}{\z@}%
                                   {-1.5pc \@plus -1ex \@minus -0.2ex}%
                                   { 0.675pc \@plus0.2ex}%
                                   {\normalfont\normalsize\sffamily\bfseries}}
\newcommand\subsection{\@startsection{subsection}{2}{\z@}%
                                     {-1.75pc\@plus -1ex \@minus -0.2ex}%
                                     { 0.025\p@\@plus 0.2ex}%
                                     {\normalfont\normalsize\sffamily\slshape}}
\newcommand\subsubsection{\@startsection{subsubsection}{3}{\z@}%
                                     {-2pc\@plus -1ex \@minus -0.2ex}%
                                     { 0.025\p@\@plus 0.2ex}%
                                     {\normalfont\normalsize\slshape}}
\def\@seccntformat#1{\csname the#1\endcsname.\quad}
\newcommand\paragraph{\@startsection{paragraph}{4}{\z@}%
                                    {3.25ex \@plus1ex \@minus.2ex}%
                                    {-1em}%
                                    {\normalfont\normalsize\itshape}}
  \setlength\leftmargini  {2.5em}
\leftmargin  \leftmargini
\setlength\leftmarginii  {2.2em}
\setlength\leftmarginiii {1.87em}
\setlength\leftmarginiv  {1.7em}
\setlength\leftmarginv   {1em}
\setlength\leftmarginvi  {1em}
\setlength  \labelsep  {0.5em}
\setlength  \labelwidth{\leftmargini}
\addtolength\labelwidth{-\labelsep}
\setlength\partopsep{2\p@ \@plus 1\p@ \@minus 1\p@}
\@beginparpenalty -\@lowpenalty
\@endparpenalty   -\@lowpenalty
\@itempenalty     -\@lowpenalty
\def\@listi{\leftmargin\leftmargini
            \parsep 4\p@ \@plus2\p@ \@minus\p@
            \topsep 8\p@ \@plus2\p@ \@minus4\p@
            \itemsep4\p@ \@plus2\p@ \@minus\p@}
\let\@listI\@listi
\@listi
\def\@listii {\leftmargin\leftmarginii
              \labelwidth\leftmarginii
              \advance\labelwidth-\labelsep
              \topsep    4\p@ \@plus2\p@ \@minus\p@
              \parsep    2\p@ \@plus\p@  \@minus\p@
              \itemsep   \parsep}
\def\@listiii{\leftmargin\leftmarginiii
              \labelwidth\leftmarginiii
              \advance\labelwidth-\labelsep
              \topsep    2\p@ \@plus\p@\@minus\p@
              \parsep    \z@
              \partopsep \p@ \@plus\z@ \@minus\p@
              \itemsep   \topsep}
\def\@listiv {\leftmargin\leftmarginiv
              \labelwidth\leftmarginiv
              \advance\labelwidth-\labelsep}
\def\@listv  {\leftmargin\leftmarginv
              \labelwidth\leftmarginv
              \advance\labelwidth-\labelsep}
\def\@listvi {\leftmargin\leftmarginvi
              \labelwidth\leftmarginvi
              \advance\labelwidth-\labelsep}
\renewcommand\theenumi{\@alph\c@enumi}
\renewcommand\theenumii{\@roman\c@enumii}
\renewcommand\theenumiii{\@Alph\c@enumiii}
\renewcommand\theenumiv{\@Alph\c@enumiv}
\newcommand\labelenumi{(\theenumi)}
\newcommand\labelenumii{(\theenumii)}
\newcommand\labelenumiii{\theenumiii.}
\newcommand\labelenumiv{\theenumiv.}
\renewcommand\p@enumii{\theenumi}
\renewcommand\p@enumiii{\theenumi(\theenumii)}
\renewcommand\p@enumiv{\p@enumiii\theenumiii}
\def\enumerate{%
  \ifnum \@enumdepth >\thr@@\@toodeep\else
    \advance\@enumdepth\@ne
    \edef\@enumctr{enum\romannumeral\the\@enumdepth}%
      \expandafter
      \list
        \csname label\@enumctr\endcsname
        {\topsep6pt\itemsep\z@\parsep\z@
         \topsep 10\p@ \@plus2\p@ \@minus4\p@
         \usecounter\@enumctr\def\makelabel##1{\hss\llap{##1}}}%
  \fi}

\newcommand\labelitemi{$\m@th\bullet$}
\newcommand\labelitemii{\normalfont\bfseries --}
\newcommand\labelitemiii{$\m@th\ast$}
\newcommand\labelitemiv{$\m@th\cdot$}
\newenvironment{description}
               {\list{}{\labelwidth\z@ \itemindent-\leftmargin
                        \let\makelabel\descriptionlabel}}
               {\endlist}
\newcommand*\descriptionlabel[1]{\hspace\labelsep
                                \normalfont\bfseries #1}
\newenvironment{abstract}{\list{}{\leftmargin=2pc\parsep\z@\itemsep 3\p@}%
     \item[]\relax
     \sffamily\footnotesize{\bfseries\abstractname}.}%
    {\ifx\@keywords\@empty
      \else
        \medskip
        \par\noindent\textsl{Keywords}: \@keywords
      \fi
     \endlist\normalsize}
\let\@keywords\@empty
\def\keyword#1{%
  \ifx\@keywords\@empty
    \def\@keywords{#1}
  \else
    \g@addto@macro\@keywords{; #1}
  \fi}
\def\keywords#1{%
  \ifx\@keywords\@empty
    \def\@keywords{#1}
  \else
    \g@addto@macro\@keywords{#1}
  \fi}
\newenvironment{verse}
               {\let\\\@centercr
                \list{}{\itemsep      \z@
                        \itemindent   -1.5em%
                        \listparindent\itemindent
                        \rightmargin  \leftmargin
                        \advance\leftmargin 1.5em}%
                \item\relax}
               {\endlist}
\newenvironment{quotation}
               {\list{}{\listparindent 1.5em%
                        \itemindent    \listparindent
                        \rightmargin   \leftmargin
                        \parsep        \z@ \@plus\p@}%
                \item\relax}
               {\endlist}
\newenvironment{quote}
               {\list{}{\rightmargin\leftmargin}%
                \item\relax}
               {\endlist}
\def\newdefinition#1{%
  \@ifnextchar[{\@odef{#1}}{\@ndef{#1}}}
\def\@ndef#1#2{%
  \@ifnextchar[{\@xndef{#1}{#2}}{\@yndef{#1}{#2}}}
\def\@xndef#1#2[#3]{%
  \expandafter\@ifdefinable\csname #1\endcsname
    {\@definecounter{#1}\@newctr{#1}[#3]%
     \expandafter\xdef\csname the#1\endcsname{%
       \expandafter\noexpand\csname the#3\endcsname \@defcountersep
          \@defcounter{#1}}%
     \global\@namedef{#1}{\@prf{#1}{#2}}%
     \global\@namedef{end#1}{\@enddefinition}}}
\def\@yndef#1#2{%
  \expandafter\@ifdefinable\csname #1\endcsname
    {\@definecounter{#1}%
     \expandafter\xdef\csname the#1\endcsname{\@defcounter{#1}}%
     \global\@namedef{#1}{\@def{#1}{#2}}%
     \global\@namedef{end#1}{\@enddefinition}}}
\def\@odef#1[#2]#3{%
  \@ifundefined{c@#2}{\@nocounterr{#2}}%
    {\expandafter\@ifdefinable\csname #1\endcsname
    {\global\@namedef{the#1}{\@nameuse{the#2}}%
  \global\@namedef{#1}{\@def{#2}{#3}}%
  \global\@namedef{end#1}{\@enddefinition}}}}
\def\@def#1#2{%
  \refstepcounter{#1}%
  \@ifnextchar[{\@ydef{#1}{#2}}{\@xdef{#1}{#2}}}
\def\@xdef#1#2{%
  \@begindefinition{#2}{\csname the#1\endcsname}\ignorespaces}
\def\@ydef#1#2[#3]{%
  \@opargbegindefinition{#2}{\csname the#1\endcsname}{#3}\ignorespaces}
\def\@defcounter#1{\noexpand\arabic{#1}}
\def\@defcountersep{.}
\def\@begindefinition#1#2{\trivlist
   \item[\hskip \labelsep{\indent\scshape #1\ #2.}]\rmfamily}
\def\@opargbegindefinition#1#2#3{\trivlist
      \item[\hskip \labelsep{\indent\scshape #1\ #2\ (#3).}]\rmfamily}
\def\@enddefinition{\endtrivlist}
\def\@begintheorem#1#2{\trivlist
   \item[\hskip \labelsep{\indent\scshape #1\ #2.}]\itshape}
\def\@opargbegintheorem#1#2#3{\trivlist
      \item[\hskip \labelsep{\indent\scshape #1\ #2\ (#3).}]\itshape}
\def\newproof#1{%
  \@ifnextchar[{\@oprf{#1}}{\@nprf{#1}}}
\def\@nprf#1#2{%
  \@ifnextchar[{\@xnprf{#1}{#2}}{\@ynprf{#1}{#2}}}
\def\@xnprf#1#2[#3]{%
  \expandafter\@ifdefinable\csname #1\endcsname
    {\@definecounter{#1}\@newctr{#1}[#3]%
     \expandafter\xdef\csname the#1\endcsname{%
       \expandafter\noexpand\csname the#3\endcsname \@prfcountersep
          \@prfcounter{#1}}%
     \global\@namedef{#1}{\@prf{#1}{#2}}%
     \global\@namedef{end#1}{\@endproof}}}
\def\@ynprf#1#2{%
  \expandafter\@ifdefinable\csname #1\endcsname
    {\@definecounter{#1}%
     \expandafter\xdef\csname the#1\endcsname{\@prfcounter{#1}}%
     \global\@namedef{#1}{\@prf{#1}{#2}}%
     \global\@namedef{end#1}{\@endproof}}}
\def\@oprf#1[#2]#3{%
  \@ifundefined{c@#2}{\@nocounterr{#2}}%
    {\expandafter\@ifdefinable\csname #1\endcsname
    {\global\@namedef{the#1}{\@nameuse{the#2}}%
  \global\@namedef{#1}{\@prf{#2}{#3}}%
  \global\@namedef{end#1}{\@endproof}}}}
\def\@prf#1#2{%
  \refstepcounter{#1}%
  \@ifnextchar[{\@yprf{#1}{#2}}{\@xprf{#1}{#2}}}
\def\@xprf#1#2{%
  \@beginproof{#2}{\csname the#1\endcsname}\ignorespaces}
\def\@yprf#1#2[#3]{%
  \@opargbeginproof{#2}{\csname the#1\endcsname}{#3}\ignorespaces}
\def\@prfcounter#1{\noexpand\arabic{#1}}
\def\@prfcountersep{.}
\def\@beginproof#1#2{\trivlist
   \item[\hskip \labelsep{\indent\scshape #1.}]\rmfamily}
\def\@opargbeginproof#1#2#3{\trivlist
      \item[\hskip \labelsep{\indent\scshape #1\ (#3).}]\rmfamily}
\def\@endproof{\endtrivlist}
\newcommand*{\qed}{\hbox{}\hfill$\Box$}
\newproof{proof}{Proof}
\newcommand\appendix{\par
  \setcounter{section}{0}%
  \setcounter{subsection}{0}%
  \renewcommand\thesection{\@Alph\c@section}}
\setlength\arraycolsep{5\p@}
\setlength\tabcolsep{6\p@}
\setlength\arrayrulewidth{0.4\p@}
\setlength\doublerulesep{0.4\p@}
\setlength\tabbingsep{\labelsep}
\skip\@mpfootins = \skip\footins
\setlength\fboxsep{3\p@}
\setlength\fboxrule{0.4\p@}
\renewcommand\theequation{\@arabic\c@equation}
\newcounter{figure}
\renewcommand\thefigure{\@arabic\c@figure}
\def\fps@figure{tbp}
\def\ftype@figure{1}
\def\ext@figure{lof}
\def\fnum@figure{\figurename~\thefigure}
\newenvironment{figure}
               {\@float{figure}}
               {\end@float}
\newcounter{table}
\renewcommand\thetable{\@arabic\c@table}
\def\fps@table{tbp}
\def\ext@table{lot}
\def\ftype@table{2}
\def\fnum@table{\tablename~\thetable}
{\catcode`\#11\relax
\gdef\hash{#}
\catcode`\#6\relax}
\def\statsocrulewidth#1#2{\@bsphack
  \protected@write\@auxout{}%
 {\string\newdimen\string\statsocwidth@#1\string\global\string\statsocwidth@#1=#2
  \string\gdef\string\statsoc@gobble\hash\string1{}}%
  \@esphack}
\gdef\statsoc@gobble#1{#1}
\newbox\statsoc@width
\newenvironment{table}
               {\@float{table}\centering%
                \footnotesize
                \addtocounter{table}{1}%
  \statsoc@gobble{%
    \expandafter\newdimen\csname statsocwidth@\alph{table}\endcsname
    \expandafter\global\csname statsocwidth@\alph{table}\endcsname0.33333\textwidth}%
                \expandafter\@tempdima\csname statsocwidth@\alph{table}\endcsname
                 \addtocounter{table}{-1}
\setlength\fboxsep{\z@}
\setlength\fboxrule{0.8\p@}
                \setbox\statsoc@width\hbox\bgroup%
                \begin{tabular}{@{}c@{}}}
                {\end{tabular}\egroup
\setlength\fboxsep{3\p@}
\setlength\fboxrule{0.4\p@}
                 \statsocrulewidth{\alph{table}}{\the\wd\statsoc@width}
                 \hskip0pt\hfill\box\statsoc@width\hfill\hskip0pt\end@float}
\newlength\abovecaptionskip
\newlength\belowcaptionskip
\setlength\abovecaptionskip{10\p@}
\setlength\belowcaptionskip{0\p@}
\xdef\statsoc@figure{figure}
\long\def\@makecaption#1#2{%
  \ifx\statsoc@figure\@captype%
  \vskip\abovecaptionskip
  \sbox\@tempboxa{\sffamily\footnotesize \textbf{#1.} #2\strut}%
  \ifdim \wd\@tempboxa >\hsize
    \sffamily\footnotesize \textbf{#1.} #2\strut\par
  \else
    \global \@minipagefalse
    \hb@xt@\hsize{\hfil\box\@tempboxa\hfil}%
  \fi
  \vskip\belowcaptionskip
  \else
  \statsoc@gobble{%
    \expandafter\newdimen\csname statsocwidth@\alph{table}\endcsname
    \expandafter\global\csname
statsocwidth@\alph{table}\endcsname0.33333\textwidth}%
    \expandafter\@tempdima\csname statsocwidth@\alph{table}\endcsname
    \parbox{\@tempdima}{\sffamily\footnotesize\textbf{#1.}
#2\strut}\aftergroup\\
  \fi}
\DeclareOldFontCommand{\rm}{\normalfont\rmfamily}{\mathrm}
\DeclareOldFontCommand{\sf}{\normalfont\sffamily}{\mathsf}
\DeclareOldFontCommand{\tt}{\normalfont\ttfamily}{\mathtt}
\DeclareOldFontCommand{\bf}{\normalfont\bfseries}{\mathbf}
\DeclareOldFontCommand{\it}{\normalfont\itshape}{\mathit}
\DeclareOldFontCommand{\sl}{\normalfont\slshape}{\@nomath\sl}
\DeclareOldFontCommand{\sc}{\normalfont\scshape}{\@nomath\sc}
\DeclareRobustCommand*\cal{\@fontswitch\relax\mathcal}
\DeclareRobustCommand*\mit{\@fontswitch\relax\mathnormal}
\newcommand\@pnumwidth{1.55em}
\newcommand\@tocrmarg{2.55em}
\newcommand\@dotsep{4.5}
\setcounter{tocdepth}{3}
\newenvironment{thebibliography}[1]
     {\section*{\refname
        \@mkboth{\MakeUppercase\refname}{\MakeUppercase\refname}}%
      \list{\@biblabel{\@arabic\c@enumiv}}%
           {\settowidth\labelwidth{\@biblabel{#1}}%
            \leftmargin\labelwidth
            \advance\leftmargin\labelsep
            \@openbib@code
            \usecounter{enumiv}%
            \let\p@enumiv\@empty
            \renewcommand\theenumiv{\@arabic\c@enumiv}}%
      \sloppy\clubpenalty4000\widowpenalty4000%
      \sfcode`\.\@m}
     {\def\@noitemerr
       {\@latex@warning{Empty `thebibliography' environment}}%
      \endlist}
\newcommand\newblock{\hskip0.11em\@plus0.33em\@minus0.07em}
\let\@openbib@code\@empty
\IfFileExists{natbib.sty}%
             {\RequirePackage[authoryear]{natbib}}%
             {\newcommand\rename{References}}
\renewcommand\footnoterule{%
  \kern-3\p@
  \hrule\@width.4\columnwidth
  \kern2.6\p@}
\def\@fnsymbol#1{\ensuremath{\ifcase#1\or \dagger\or \ddagger\or
   \mathsection\or \mathparagraph\or \|\or **\or \dagger\dagger
   \or \ddagger\ddagger\or \mathsection\mathsection
   \or \mathparagraph\mathparagraph \else\@ctrerr\fi}}
\def\fnsymbol#1{\expandafter\@fnsymbol\csname #1\endcsname}
\def\thefootnote{\@fnsymbol\c@footnote}
\newlength\fnsymbolwidth
\setlength{\fnsymbolwidth}{1.3em}
\newcommand\@makefntext[1]{%
    \parindent 1em%
    \noindent
    \hb@xt@\fnsymbolwidth{\hss\@makefnmark}#1}
\renewcommand\@makefnmark{\hbox{{\normalfont\@thefnmark}}}
\renewcommand\refname{References}
\newcommand\figurename{Fig.}
\newcommand\tablename{Table}
\newcommand\appendixname{Appendix}
\newcommand\abstractname{Summary}
\newcommand\today{}
\edef\today{\ifcase\month\or
  January\or February\or March\or April\or May\or June\or
  July\or August\or September\or October\or November\or December\fi
  \space\number\day, \number\year}
\pagestyle{statsoc}
\pagenumbering{arabic}
%%%%
\def\keywords#1{\noindent\begin{quote}\sffamily\small\textsl{Keywords}: #1\end{quote}}
%%
\renewcommand\normalsize{%
   \@setfontsize\normalsize{11}{13}
   \abovedisplayskip 10\p@ \@plus2\p@ \@minus5\p@
   \abovedisplayshortskip \z@ \@plus3\p@
   \belowdisplayshortskip 6\p@ \@plus3\p@ \@minus3\p@
   \belowdisplayskip \abovedisplayskip
   \let\@listi\@listI}
\renewcommand\small{%
   \@setfontsize\small{10}{12}%
   \abovedisplayskip 8.5\p@ \@plus3\p@ \@minus4\p@
   \abovedisplayshortskip \z@ \@plus2\p@
   \belowdisplayshortskip 4\p@ \@plus2\p@ \@minus2\p@
   \def\@listi{\leftmargin\leftmargini
               \topsep 4\p@ \@plus2\p@ \@minus2\p@
               \parsep 2\p@ \@plus\p@ \@minus\p@
               \itemsep \parsep}%
   \belowdisplayskip \abovedisplayskip
}
\renewcommand\footnotesize{%
   \@setfontsize\footnotesize{9}{11}%
   \abovedisplayskip 6\p@ \@plus2\p@ \@minus4\p@
   \abovedisplayshortskip \z@ \@plus\p@
   \belowdisplayshortskip 3\p@ \@plus\p@ \@minus2\p@
   \def\@listi{\leftmargin\leftmargini
               \topsep 3\p@ \@plus\p@ \@minus\p@
               \parsep 2\p@ \@plus\p@ \@minus\p@
               \itemsep \parsep}%
   \belowdisplayskip \abovedisplayskip
}
%%%%
