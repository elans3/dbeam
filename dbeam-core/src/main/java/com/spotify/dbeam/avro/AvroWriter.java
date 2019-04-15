/*-
 * -\-\-
 * DBeam Core
 * --
 * Copyright (C) 2016 - 2019 Spotify AB
 * --
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -/-/-
 */

package com.spotify.dbeam.avro;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;

import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AvroWriter implements Runnable {

  private static final Logger LOGGER = LoggerFactory.getLogger(AvroWriter.class);
  private final DataFileWriter<GenericRecord> dataFileWriter;
  private final BlockingQueue<ByteBuffer> queue;

  public AvroWriter(
      DataFileWriter<GenericRecord> dataFileWriter,
      BlockingQueue<ByteBuffer> queue) {
    this.dataFileWriter = dataFileWriter;
    this.queue = queue;
  }

  @Override
  public void run() {
    LOGGER.debug("AvroWriter started");
    try {
      int c = 0;
      while (true) {
        final ByteBuffer datum = queue.take();
        if (datum.capacity() == 0) {
          this.dataFileWriter.sync();
          return;
        } else {
          this.dataFileWriter.appendEncoded(datum);
          c++;
        }
        if ((c % 100000) == 0) {
          LOGGER.info("Size={} remainingCapacity={}", queue.size(), queue.remainingCapacity());
        }
      }
    } catch (InterruptedException ex) {
      LOGGER.warn("AvroWriter interrupted");
    } catch (IOException e) {
      LOGGER.error("Error on AvroWriter", e);
    }
  }
}