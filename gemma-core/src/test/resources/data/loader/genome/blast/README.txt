#formatdb -i ../testsequence.fa -n testblastdb -o T
#formatdb -i ../gpl140.sequences.fasta -n testblastdbPartTwo -o T
makeblastdb  -parse_seqids  -in ../testsequence.fa -out testblastdb -dbtype nucl
makeblastdb -parse_seqids -in ../gpl140.sequences.fasta -out testblastdbPartTwo -dbtype nucl

#blastdbcmd -db testblastdb -entry "M63012.1" -long_seqids
#blastdbcmd -long_seqids -db testblastdb -entry_batch  /var/tmp/file/with/list/of/accs
