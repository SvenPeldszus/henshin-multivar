package org.eclipse.emf.henshin.interpreter.ui.giraph;

public class HenshinUtilTemplate
{
  protected static String nl;
  public static synchronized HenshinUtilTemplate create(String lineSeparator)
  {
    nl = lineSeparator;
    HenshinUtilTemplate result = new HenshinUtilTemplate();
    nl = null;
    return result;
  }

  public final String NL = nl == null ? (System.getProperties().getProperty("line.separator")) : nl;
  protected final String TEXT_1 = "/*" + NL + " * Licensed to the Apache Software Foundation (ASF) under one" + NL + " * or more contributor license agreements.  See the NOTICE file" + NL + " * distributed with this work for additional information" + NL + " * regarding copyright ownership.  The ASF licenses this file" + NL + " * to you under the Apache License, Version 2.0 (the" + NL + " * \"License\"); you may not use this file except in compliance" + NL + " * with the License.  You may obtain a copy of the License at" + NL + " *" + NL + " *     http://www.apache.org/licenses/LICENSE-2.0" + NL + " *" + NL + " * Unless required by applicable law or agreed to in writing, software" + NL + " * distributed under the License is distributed on an \"AS IS\" BASIS," + NL + " * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied." + NL + " * See the License for the specific language governing permissions and" + NL + " * limitations under the License." + NL + " */" + NL + "package org.apache.giraph.examples;" + NL + "" + NL + "import java.io.IOException;" + NL + "import java.util.Arrays;" + NL + "import java.util.List;" + NL + "" + NL + "import org.apache.giraph.edge.Edge;" + NL + "import org.apache.giraph.edge.EdgeFactory;" + NL + "import org.apache.giraph.graph.Vertex;" + NL + "import org.apache.giraph.io.formats.TextVertexInputFormat;" + NL + "import org.apache.giraph.io.formats.TextVertexOutputFormat;" + NL + "import org.apache.hadoop.io.BytesWritable;" + NL + "import org.apache.hadoop.io.IntWritable;" + NL + "import org.apache.hadoop.io.Text;" + NL + "import org.apache.hadoop.mapreduce.InputSplit;" + NL + "import org.apache.hadoop.mapreduce.TaskAttemptContext;" + NL + "import org.json.JSONArray;" + NL + "import org.json.JSONException;" + NL + "" + NL + "import com.google.common.collect.Lists;" + NL + "" + NL + "/**" + NL + " * Henshin utility classes and methods." + NL + " */" + NL + "public class HenshinUtil {" + NL + "" + NL + "  /**" + NL + "   * Private constructor." + NL + "   */" + NL + "  private HenshinUtil() {" + NL + "    // Prevent instantiation" + NL + "  }" + NL + "" + NL + "  /**" + NL + "   * Henshin data." + NL + "   */" + NL + "  public abstract static class Bytes extends BytesWritable {" + NL + "" + NL + "    /**" + NL + "     * Default constructor." + NL + "     */" + NL + "    public Bytes() {" + NL + "      super();" + NL + "    }" + NL + "" + NL + "    /**" + NL + "     * Extra constructor." + NL + "     * @param data The data." + NL + "     */" + NL + "    public Bytes(byte[] data) {" + NL + "      super(data);" + NL + "    }" + NL + "" + NL + "    /**" + NL + "     * Set the size." + NL + "     * @param size The new size." + NL + "     */" + NL + "    @Override" + NL + "    public void setSize(int size) {" + NL + "      if (size != getCapacity()) {" + NL + "        setCapacity(size);" + NL + "      }" + NL + "      super.setSize(size);" + NL + "    }" + NL + "" + NL + "    /**" + NL + "     * Pretty-print this bytes object." + NL + "     * @return The printed string." + NL + "     */" + NL + "    @Override" + NL + "    public String toString() {" + NL + "      byte[] bytes = getBytes();" + NL + "      StringBuffer result = new StringBuffer();" + NL + "      for (int i = 0; i < bytes.length; i++) {" + NL + "        result.append(bytes[i]);" + NL + "        if (i < bytes.length - 1) {" + NL + "          result.append(\",\");" + NL + "        }" + NL + "      }" + NL + "      return \"[\" + result + \"]\";" + NL + "    }" + NL + "" + NL + "  }" + NL + "" + NL + "  /**" + NL + "   * Henshin match object." + NL + "   */" + NL + "  public static class Match extends Bytes {" + NL + "" + NL + "    /**" + NL + "     * Empty match." + NL + "     */" + NL + "    public static final Match EMPTY = new Match();" + NL + "" + NL + "    /**" + NL + "     * Default constructor." + NL + "     */" + NL + "    public Match() {" + NL + "      super();" + NL + "    }" + NL + "" + NL + "    /**" + NL + "     * Extra constructor." + NL + "     * @param data The data." + NL + "     */" + NL + "    public Match(byte[] data) {" + NL + "      super(data);" + NL + "    }" + NL + "" + NL + "    /**" + NL + "     * Get the vertex ID of a matched node." + NL + "     * @param vertexIndex Index of the next vertex." + NL + "     * @return The vertex ID." + NL + "     */" + NL + "    public VertexId getVertexId(int vertexIndex) {" + NL + "      byte[] bytes = getBytes();" + NL + "      int d = 0;" + NL + "      for (int i = 0; i < vertexIndex; i++) {" + NL + "        d += bytes[d] + 1;" + NL + "      }" + NL + "      return new VertexId(" + NL + "        Arrays.copyOfRange(bytes, d + 1, d + 1 + bytes[d]));" + NL + "    }" + NL + "" + NL + "    /**" + NL + "     * Create an extended version of this (partial) match." + NL + "     * @param vertexId The ID of the next matched vertex." + NL + "     * @return The extended match object." + NL + "     */" + NL + "    public Match extend(VertexId vertexId) {" + NL + "      byte[] bytes = getBytes();" + NL + "      byte[] id = vertexId.getBytes();" + NL + "      byte[] result = Arrays.copyOf(bytes, bytes.length + 1 + id.length);" + NL + "      result[bytes.length] = (byte) id.length;" + NL + "      System.arraycopy(id, 0, result, bytes.length + 1, id.length);" + NL + "      return new Match(result);" + NL + "    }" + NL + "" + NL + "    /**" + NL + "     * Pretty-print this match." + NL + "     * @return The printed string." + NL + "     */" + NL + "    @Override" + NL + "    public String toString() {" + NL + "      byte[] bytes = getBytes();" + NL + "      StringBuffer result = new StringBuffer();" + NL + "      int i = 0;" + NL + "      while (i < bytes.length) {" + NL + "        int len = bytes[i++];" + NL + "        result.append(\"[\");" + NL + "        for (int j = 0; j < len; j++) {" + NL + "          result.append(bytes[i + j]);" + NL + "          if (j < len - 1) {" + NL + "            result.append(\",\");" + NL + "          }" + NL + "        }" + NL + "        result.append(\"]\");" + NL + "        if (i < bytes.length - 1) {" + NL + "          result.append(\",\");" + NL + "        }" + NL + "        i += len;" + NL + "      }" + NL + "      return \"[\" + result + \"]\";" + NL + "    }" + NL + "" + NL + "  }" + NL + "" + NL + "  /**" + NL + "   * Henshin vertex ID." + NL + "   */" + NL + "  public static class VertexId extends Bytes {" + NL + "" + NL + "    /**" + NL + "     * Default constructor." + NL + "     */" + NL + "    public VertexId() {" + NL + "      super();" + NL + "    }" + NL + "" + NL + "    /**" + NL + "     * Extra constructor." + NL + "     * @param data The data." + NL + "     */" + NL + "    public VertexId(byte[] data) {" + NL + "      super(data);" + NL + "    }" + NL + "" + NL + "    /**" + NL + "     * Create an extended version of this vertex ID." + NL + "     * @param value The value to be appended to this vertex ID." + NL + "     * @return The extended version of this vertex ID." + NL + "     */" + NL + "    public VertexId extend(byte value) {" + NL + "      byte[] bytes = getBytes();" + NL + "      bytes = Arrays.copyOf(bytes, bytes.length + 1);" + NL + "      bytes[bytes.length - 1] = value;" + NL + "      return new VertexId(bytes);" + NL + "    }" + NL + "" + NL + "  }" + NL + "" + NL + "  /**" + NL + "   * Henshin input format." + NL + "   */" + NL + "  public static class InputFormat extends" + NL + "    TextVertexInputFormat<VertexId, IntWritable, IntWritable> {" + NL + "" + NL + "    @Override" + NL + "    public TextVertexReader createVertexReader(InputSplit split," + NL + "      TaskAttemptContext context) {" + NL + "      return new InputReader();" + NL + "    }" + NL + "" + NL + "    /**" + NL + "     * Henshin input reader." + NL + "     */" + NL + "    class InputReader extends" + NL + "      TextVertexReaderFromEachLineProcessedHandlingExceptions<JSONArray," + NL + "        JSONException> {" + NL + "" + NL + "      @Override" + NL + "      protected JSONArray preprocessLine(Text line) throws JSONException {" + NL + "        return new JSONArray(line.toString());" + NL + "      }" + NL + "" + NL + "      @Override" + NL + "      protected VertexId getId(JSONArray jsonVertex)" + NL + "        throws JSONException, IOException {" + NL + "        return jsonArrayToVertexId(jsonVertex.getJSONArray(0));" + NL + "      }" + NL + "" + NL + "      /**" + NL + "       * Convert a JSON array to a VertexId object." + NL + "       * @param jsonArray The JSON array to be converted." + NL + "       * @return The corresponding VertexId." + NL + "       */" + NL + "      private VertexId jsonArrayToVertexId(JSONArray jsonArray)" + NL + "        throws JSONException {" + NL + "        byte[] bytes = new byte[jsonArray.length()];" + NL + "        for (int i = 0; i < bytes.length; i++) {" + NL + "          bytes[i] = (byte) jsonArray.getInt(i);" + NL + "        }" + NL + "        return new VertexId(bytes);" + NL + "      }" + NL + "" + NL + "      @Override" + NL + "      protected IntWritable getValue(JSONArray jsonVertex)" + NL + "        throws JSONException, IOException {" + NL + "        return new IntWritable(jsonVertex.getInt(1));" + NL + "      }" + NL + "" + NL + "      @Override" + NL + "      protected Iterable<Edge<VertexId, IntWritable>> getEdges(" + NL + "        JSONArray jsonVertex) throws JSONException, IOException {" + NL + "        JSONArray jsonEdgeArray = jsonVertex.getJSONArray(2);" + NL + "        List<Edge<VertexId, IntWritable>> edges =" + NL + "          Lists.newArrayListWithCapacity(jsonEdgeArray.length());" + NL + "        for (int i = 0; i < jsonEdgeArray.length(); ++i) {" + NL + "          JSONArray jsonEdge = jsonEdgeArray.getJSONArray(i);" + NL + "          edges.add(EdgeFactory.create(jsonArrayToVertexId(" + NL + "            jsonEdge.getJSONArray(0)), new IntWritable(jsonEdge.getInt(1))));" + NL + "        }" + NL + "        return edges;" + NL + "      }" + NL + "" + NL + "      @Override" + NL + "      protected Vertex<VertexId, IntWritable, IntWritable>" + NL + "      handleException(Text line, JSONArray jsonVertex, JSONException e) {" + NL + "        throw new IllegalArgumentException(" + NL + "          \"Couldn't get vertex from line \" + line, e);" + NL + "      }" + NL + "    }" + NL + "  }" + NL + "" + NL + "  /**" + NL + "   * Henshin output format." + NL + "   */" + NL + "  public static class OutputFormat extends" + NL + "    TextVertexOutputFormat<VertexId, IntWritable, IntWritable> {" + NL + "" + NL + "    @Override" + NL + "    public TextVertexWriter createVertexWriter(TaskAttemptContext context)" + NL + "      throws IOException, InterruptedException {" + NL + "      return new OutputWriter();" + NL + "    }" + NL + "" + NL + "    /**" + NL + "     * Henshin output writer." + NL + "     */" + NL + "    class OutputWriter extends TextVertexWriterToEachLine {" + NL + "" + NL + "      @Override" + NL + "      protected Text convertVertexToLine(" + NL + "        Vertex<VertexId, IntWritable, IntWritable> vertex)" + NL + "        throws IOException {" + NL + "" + NL + "        JSONArray vertexArray = new JSONArray();" + NL + "        JSONArray idArray = new JSONArray();" + NL + "        byte[] id = vertex.getId().getBytes();" + NL + "        for (int i = 0; i < id.length; i++) {" + NL + "          idArray.put(id[i]);" + NL + "        }" + NL + "        vertexArray.put(idArray);" + NL + "        vertexArray.put(vertex.getValue().get());" + NL + "        JSONArray allEdgesArray = new JSONArray();" + NL + "        for (Edge<VertexId, IntWritable> edge : vertex.getEdges()) {" + NL + "          JSONArray edgeArray = new JSONArray();" + NL + "          JSONArray targetIdArray = new JSONArray();" + NL + "          byte[] targetId = edge.getTargetVertexId().getBytes();" + NL + "          for (int i = 0; i < targetId.length; i++) {" + NL + "            targetIdArray.put(targetId[i]);" + NL + "          }" + NL + "          edgeArray.put(targetIdArray);" + NL + "          edgeArray.put(edge.getValue().get());" + NL + "          allEdgesArray.put(edgeArray);" + NL + "        }" + NL + "        vertexArray.put(allEdgesArray);" + NL + "        return new Text(vertexArray.toString());" + NL + "      }" + NL + "    }" + NL + "  }" + NL + "" + NL + "}";
  protected final String TEXT_2 = NL;

  public String generate(Object argument)
  {
    final StringBuffer stringBuffer = new StringBuffer();
    stringBuffer.append(TEXT_1);
    stringBuffer.append(TEXT_2);
    return stringBuffer.toString();
  }
}
