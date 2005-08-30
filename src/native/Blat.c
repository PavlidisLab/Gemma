#include "include/gfClient.h"
#include <jni.h>
#include <stdio.h>

/*
 * 
 */
JNIEXPORT jobject JNICALL Java_edu_columbia_gemma_tools_Blat_GfClientCall
(JNIEnv * env, jobject obj, jstring jhostname, jstring jport, jstring jseqDir, jstring jqueryFile, jstring joutputFile) {
  
  const char *hostname = (*env)->GetStringUTFChars(env, jhostname, 0);
  const char *port =  (*env)->GetStringUTFChars(env, jport, 0);
  const char *seqDir =  (*env)->GetStringUTFChars(env, jseqDir, 0);
  const char *queryFile = (*env)->GetStringUTFChars(env, jqueryFile, 0);
  const char *outputFile = (*env)->GetStringUTFChars(env, joutputFile, 0);
  
  gfClient(hostname, port, seqDir, queryFile, outputFile, "dna", "dna");
  printf("Hello world\n");
  (*env)->ReleaseStringUTFChars(env, jhostname, hostname);
  (*env)->ReleaseStringUTFChars(env, jport, port);
  (*env)->ReleaseStringUTFChars(env, jseqDir, seqDir);
  (*env)->ReleaseStringUTFChars(env, jqueryFile, queryFile);
  (*env)->ReleaseStringUTFChars(env, joutputFile, outputFile);
  return;
}

/* this is here to make cygwin happy... oh well, not needed afterall when we link to the gfClient lib */
//int main (int argc, char **argv) {}
/*void gfClient(const char *hostName, const char *portName, const char *tSeqDir, const char *inName,
	      const char *outName, const char *tTypeName, const char *qTypeName) {
  fprintf(stdout, "wow!\n");
}
*/
