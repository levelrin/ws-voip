class AudioInputProcess extends AudioWorkletProcessor {

    /**
     * True means sending the audio data to the WebSocket server.
     * @type {boolean}
     */
    active = false;

    constructor() {
        super();
        this.port.onmessage = this.handleMessage.bind(this);
    }

    handleMessage(event) {
        this.active = event.data;
    }

    /**
     * https://developer.mozilla.org/en-US/docs/Web/API/Web_Audio_API/Using_AudioWorklet
     * @param inputList Number of active microphones.
     *                  The length is usually one because other microphones are deactivated.
     * @param _ We are not using this.
     * @param __ We are not using this.
     * @return {boolean}
     */
    process(inputList, _, __) {
        if (this.active) {
            // todo: compress the data.
            this.port.postMessage(inputList);
        }
        return true;
    }

}

registerProcessor("audioInputProcess", AudioInputProcess);
