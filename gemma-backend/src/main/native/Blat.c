#include "include/gfClient.h"
#include <jni.h>
#include <stdio.h>

/*
 * 
 */
JNIEXPORT jobject JNICALL Java_ubic_gemma_apps_Blat_GfClientCall
  (JNIEnv * env, jobject obj, jstring jhostname, jstring jport,
   jstring jseqDir, jstring jqueryFile, jstring joutputFile)
{

  const char *hostname = (*env)->GetStringUTFChars (env, jhostname, 0);
  const char *port = (*env)->GetStringUTFChars (env, jport, 0);
  const char *seqDir = (*env)->GetStringUTFChars (env, jseqDir, 0);
  const char *queryFile = (*env)->GetStringUTFChars (env, jqueryFile, 0);
  const char *outputFile = (*env)->GetStringUTFChars (env, joutputFile, 0);
  gfClient (hostname, port, seqDir, queryFile, outputFile, "dna", "dna");

  (*env)->ReleaseStringUTFChars (env, jhostname, hostname);
  (*env)->ReleaseStringUTFChars (env, jport, port);
  (*env)->ReleaseStringUTFChars (env, jseqDir, seqDir);
  (*env)->ReleaseStringUTFChars (env, jqueryFile, queryFile);
  (*env)->ReleaseStringUTFChars (env, joutputFile, outputFile);
  return;
}
