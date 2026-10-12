
package com.org.dev.payment.service;

import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 2, jvmArgsAppend = {"-Xms1g", "-Xmx1g"})
public class VerifySignatureBenchMark {

    String keySecret;
    JSONObject signatureParams;

    @Setup(Level.Iteration)
    public void setup() {
        keySecret = "test_secret_key_for_benchmarking";
        signatureParams = new JSONObject();
        signatureParams.put("razorpay_order_id", "order_TestOrderId001");
        signatureParams.put("razorpay_payment_id", "pay_TestPaymentId01");
        signatureParams.put("razorpay_signature", "aabbccddeeff00112233445566778899");
    }

    @Benchmark
    public boolean verifySignature() throws RazorpayException {
        return Utils.verifyPaymentSignature(signatureParams, keySecret);
    }
}
