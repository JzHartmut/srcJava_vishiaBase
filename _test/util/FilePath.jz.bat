jzcmd %0
exit /B

==JZcmd==

Class FilePath = org.vishia.util.FilePath;

Filepath fileWildcards = path/**/*.ext2;



main(){
  Stringjar path;
  fileWildcards.localfileReplwildcard(path.buffer(), Filepath: local/name.ext);
  <+out><&path><.+n>

}