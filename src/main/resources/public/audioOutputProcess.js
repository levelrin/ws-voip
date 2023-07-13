class AudioOutputProcess extends AudioWorkletProcessor {

    inputListQueue = [];

    constructor() {
        super();
        this.port.onmessage = this.handleMessage.bind(this);
    }

    handleMessage(event) {
        this.inputListQueue.push(event.data);
    }

    /**
     * https://developer.mozilla.org/en-US/docs/Web/API/Web_Audio_API/Using_AudioWorklet
     * @param _ We are not using this parameter.
     * @param outputList Number of active speakers/headphones.
     * @param __ We are not suing this parameter.
     * @return {boolean}
     */
    process(_, outputList, __) {
        if (this.inputListQueue.length === 0) {
            return true;
        }

        const inputList = this.inputListQueue.shift();
        const sourceLimit = Math.min(inputList.length, outputList.length);
        for (let inputIndex = 0; inputIndex < sourceLimit; inputIndex++) {
            const input = inputList[inputIndex];
            const output = outputList[inputIndex];
            // There are usually two channels. One for left ear, another for right.
            const channelLimit = Math.min(input.length, output.length);
            for (let channelIndex = 0; channelIndex < channelLimit; channelIndex++) {
                const inputChannel = input[channelIndex];
                const outputChannel = output[channelIndex];
                // There are 128 samples by the spec at the time of writing.
                // It might be changed in the future, though.
                // Note that the for-loop using index incrementation like above won't work
                // because inputChannel is not array. It's a JSON object where key is used as index like this:
                // {"0":-0.011914870701730251,"1":-0.01202797144651413, ... "127":-0.016677042469382286}
                for (let sampleIndex in inputChannel) {
                    // 32-bit floating point number
                    const inputSample = inputChannel[sampleIndex];

                    // play the sample by setting the output values.
                    outputChannel[sampleIndex] = inputSample;

                }
            }
        }
        return true;
    }

}

registerProcessor("audioOutputProcess", AudioOutputProcess);
